package networkConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;

import encryptionDecryption.FileCrypter;
import exceptions.CouldNotDecryptMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;
import exceptions.VerificationFailedException;
import frame.Configuration;
import keyStore.SimpleKeyStore;
import messengerSystem.Authentication;
import messengerSystem.MessageSystem;

/**
 * Low level handling of {@linkplain NetworkPackage}s received by a {@linkplain ConnectionEndpoint}. <br>
 * @implNote Not all handling of {@linkplain NetworkPackage}s can be done here, since some (e.g. connection request confirmations)
 * require setting of private fields, however, this still allows to reduce the bloat of the {@linkplain ConnectionEndpoint} class somewhat.
 * @author Sasha Petri, Jonas H�hne
 */
public class NetworkPackageHandler {

	
	/** Whether files that do not have a valid signature should be saved to the system */
	final static boolean saveUnverifiedFiles = true;
	
	/**
	 * Processes a NetworkPackage.
	 * @param ce
	 * 		the ConnectionEndpoint that received the package
	 * @param msg
	 * 		the network package to process
	 * @throws EndpointIsNotConnectedException 
	 * 		can be thrown during key generation <br>
	 * 		when processing a message would lead to sending a message back, but this endpoint is not connected to their partner
	 * @throws CouldNotDecryptMessageException 
	 * 		thrown if an encrypted message is received and it could not be decrypted for some reason
	 * @throws VerificationFailedException 
	 * 		thrown if a signed message is received, but the signature was not valid for the message (verification failed)
	 */
	public static void handlePackage(ConnectionEndpoint ce, NetworkPackage msg) throws EndpointIsNotConnectedException, CouldNotDecryptMessageException, VerificationFailedException {
		
		// if it is a signed message, check the signature first 
		if (msg.getSignature() != null) {
			if (!msg.verify(MessageSystem.getAuthenticator(), ce.getID())) {
				throw new VerificationFailedException("Could not verify the text message with ID " 
						+ Base64.getEncoder().encodeToString(msg.getID()) + " using the public key of " + ce.getID());
			}
		}
		
		TransmissionTypeEnum msgType = msg.getType();
		
		switch (msgType) {
		case CONNECTION_CONFIRMATION:
			// Handled in ConnectionEndpoint
			break;
		case CONNECTION_REQUEST:
			// CESH will respond to this
			break;
		case CONNECTION_TERMINATION:
			//This is received if the other, connected connectionEndpoint wishes to close the connection. Takes all necessary actions on this local side of the connection.
			ce.forceCloseConnection();
			break;
		case FILE_TRANSFER:
			handleFile(ce, msg);
			break;
		case KEYGEN_SOURCE_SIGNAL:
			//This is only used for signaling the source server to start sending photons. 
			//TODO: Add source logic. It just needs to drop a file containing the message content as Text in a special folder where the source is waiting for the file.
			break;
		case KEYGEN_SYNC_ACCEPT:
			//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncConfirm is added to the regular messagesStack and read by the KeyGenerator.
			ce.getKeyGen().updateAccRejState(1);
			break;
		case KEYGEN_SYNC_REJECT:
			//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncReject is added to the regular messagesStack and read by the KeyGenerator.
			ce.getKeyGen().updateAccRejState(-1);
			break;
		case KEYGEN_SYNC_REQUEST:
			ce.getKeyGen().keyGenSyncResponse(msg);
			break;
		case KEYGEN_TERMINATION:
			//This is received if the connected ConnectionEndpoint intends to terminate the KeyGen Process. This will cause a local shutdown in response.
			//Terminating Key Gen
			ce.getKeyGen().shutdownKeyGen(false, true);
			break;
		case KEYGEN_TRANSMISSION:
			ce.getKeyGen().writeKeyGenFile(msg);
			break;
		case RECEPTION_CONFIRMATION: // content of messages of this type is the ID they are confirming
			// TODO check if we actually sent out a message with that ID (this will need us to also log which messages we sent...)
			// TODO theoretically these messages should be verified
			System.out.println("[" + ce.getID() + "] Received confirmation for message with ID " + Base64.getEncoder().encodeToString(msg.getContent()));
			// if we did, add the messageID to the list
			ce.addConfirmationFor(msg.getContent());
			// if we have been waiting for this ID to be confirmed before pushing a message
			// (e.g. in-order transfer, key synchronization, ...) push the waiting message now
			NetworkPackage toPush = ce.removeFromPushQueue(msg.getContent());
			if (toPush != null) ce.pushMessage(toPush);
			break;
		case TEXT_MESSAGE:
			handleTextMessage(ce, msg);
			break;
		case KEY_USE_ALERT:
			int encStartIndex = msg.getMessageArgs().keyIndex(); // index at which sender wants to start encrypting
			try {
				int ownIndex = SimpleKeyStore.getIndex(ce.getKeyStoreID());
				// check if this >= our own index
				if (encStartIndex >= ownIndex) {
					// if it is, everything is good (we set our index to A's index after encryption)
					int incrementAmount = (encStartIndex + MessageSystem.getCipher().getKeyLength() / 8) - ownIndex;
					SimpleKeyStore.incrementIndex(ce.getKeyStoreID(), incrementAmount);
					// Send back affirming message
					NetworkPackage affirmation = new NetworkPackage(TransmissionTypeEnum.KEY_USE_ACCEPT, new MessageArgs(), msg.getID(), false);
					affirmation.sign(MessageSystem.getAuthenticator());
					ce.pushMessage(affirmation);
				} else {
					// if it is not, this means the other party would use key bits that we already marked as used
					// send a message back telling the other party not to use these bits, and what our current index is
					// (if received, this allows our keys to sync up again)
					NetworkPackage denial = new NetworkPackage(TransmissionTypeEnum.KEY_USE_REJECT, new MessageArgs(ownIndex), msg.getID(), false);
					denial.sign(MessageSystem.getAuthenticator());
					ce.pushMessage(denial);
				}
			} catch (NoKeyForContactException e) {
				// Control flow wise, this should not occur - a KEY_USE_ALERT should only be able to be sent if there is a mutual key
				// in any case, log this (TODO)
			} catch (SQLException e) {
				// Nothing we can really do here except log it, possible expansion would be a special message type to the partner
				// TODO log
			}
			break;
		case KEY_USE_ACCEPT:
			// push the package we've been waiting to push
			NetworkPackage encPackageToPush = ce.removeFromPushQueue(msg.getContent());
			if (encPackageToPush != null) ce.pushMessage(encPackageToPush);
			break;
		case KEY_USE_REJECT:
			// remove the package we've been waiting to push
			NetworkPackage rejectedPackage = ce.removeFromPushQueue(msg.getContent());
			if (rejectedPackage == null) {
				// If no such package exists, that means we've been sent an unwanted KEY_USE_REJECT
				// log this as an unusual event (possibly indicates a control flow issue) but otherwise do nothing
				// TODO log
			} else {
				try {
					// Because we never sent our message, we can actually mark the bits used for encryption
					// as unused again, and then set the index to max(k, i), where k is the key index before
					// we tried encrypting, and i is the index given to us by our partner (the local key index of our partner)
					// that way, next time we will be starting at an index that is >= our partners (i.e. unused bytes)
					int k = rejectedPackage.getMessageArgs().keyIndex();
					int i = msg.getMessageArgs().keyIndex();
					int newIndex = Math.max(k, i);
					SimpleKeyStore.setIndex(ce.getKeyStoreID(), newIndex);
				} catch (NoKeyForContactException e) {
					// Control flow wise, this should not occur - a KEY_USE_ALERT should only be able to be sent if there is a mutual key
					// in any case, log this (TODO)
				} catch (SQLException e) {
					// Nothing we can really do here except log it, possible expansion would be a special message type to the partner
					// TODO log
				}
			}
			break;
		default:
			// TODO log that a message of an invalid type was received
			break;
		}

	}
	
	/*
	 * The two methods below could theoretically go into MessageSystem with some refactoring?
	 */
	
	private static void handleTextMessage(ConnectionEndpoint ce, NetworkPackage msg) throws CouldNotDecryptMessageException, VerificationFailedException {
		if (msg.getSignature() != null) { // if the message is signed, verify it
			Authentication authenticator = MessageSystem.getAuthenticator(); // the auth currently in use by the message system
			if (msg.verify(authenticator, ce.getID())) {
				// If the message is also encrypted, try to decrypt it
				if (msg.getMessageArgs().keyIndex() != -1) {
					try {
						byte[] decryptionKey = getKey(ce, msg);
						// Decrypt
						byte[] decryptedMsg 	= MessageSystem.getCipher().decrypt(msg.getContent(), decryptionKey);
						String decryptedString	= MessageSystem.byteArrayToString(decryptedMsg);
						// Log the decrypted text of the message
						ce.appendMessageToChatLog(false, decryptedString);
					} catch (SQLException | InvalidKeyException | BadPaddingException | NotEnoughKeyLeftException | NoKeyForContactException e) {
						throw new CouldNotDecryptMessageException("Could not decrypt the text message with ID " + Base64.getEncoder().encodeToString(msg.getID()), e);
					}
				} 
				// If the message is not encrypted
				else { 
					// It has already been verified, so we just log it
					ce.appendMessageToChatLog(false, MessageSystem.byteArrayToString(msg.getContent()));
					return;
				}
			} else {
				
			}
		} else {
			ce.appendMessageToChatLog(false, MessageSystem.byteArrayToString(msg.getContent()));
		}
	}
	
	private static void handleFile(ConnectionEndpoint ce, NetworkPackage msg) throws CouldNotDecryptMessageException {
		// Save the file if saving unverified files is true, or if it can be verified
		Path outDirectory = Paths.get("");
		String fileName = "";
		if (saveUnverifiedFiles || (msg.getSignature() != null && msg.verify(MessageSystem.getAuthenticator(), ce.getID()))) {
			// if the file can be verified, save it in a folder named after the connection
			outDirectory = Paths.get(Configuration.getBaseDirPath(), "ReceivedFiles" , ce.getRemoteName());
			fileName = msg.getMessageArgs().fileName();
			try {
				Files.createDirectories(outDirectory);
				// if the file name already exists, append a random integer to make it unique
				Random r = new Random();
				while (Files.exists(outDirectory.resolve(fileName))) fileName += r.nextInt(0, 10);
				File f = Files.write(outDirectory.resolve(fileName), msg.getContent()).toFile();
				
				// If it is encrypted, decrypt it
				if (msg.getMessageArgs().keyIndex() != -1) {
					// if it is encrypted, decrypt and save it
					try {
						byte[] decryptionKey = getKey(ce, msg);
						SecretKey sk = MessageSystem.getCipher().byteArrayToSecretKey(decryptionKey);
						// save decrypted file with the same filename, but prefixed with decrypted_
						Path pathToDecryptedFile = Paths.get(f.getParent().toString(), "decrypted_" + f.getName()); 
						FileCrypter.decryptAndSave(outDirectory.resolve(fileName).toFile(), MessageSystem.getCipher(), sk, pathToDecryptedFile);
					} catch (BadPaddingException | InvalidKeyException | SQLException | NoKeyForContactException | NotEnoughKeyLeftException | IOException e) {
						throw new CouldNotDecryptMessageException("Could not decrypt the text message with ID " + Base64.getEncoder().encodeToString(msg.getID()), e);
					}
				} 
			} catch (IOException e) {
				// TODO Log & exit
				return;
			}
		} else {
			// TODO Log & Exit
			return;
		}

	}
	
	/**
	 * Gets the key to be used to decrypt the passed message.
	 * @param ce
	 * @param msg
	 * @return
	 * @throws NotEnoughKeyLeftException 
	 * @throws NoKeyForContactException 
	 * @throws SQLException 
	 */
	private static byte[] getKey(ConnectionEndpoint ce, NetworkPackage msg) throws SQLException, NoKeyForContactException, NotEnoughKeyLeftException {
		// Get the key for decryption
		if (MessageSystem.getCipher().getKeyLength() % 8 != 0) { // check needed, because SimpleKeyStore only supports byte-sized keys
			// TODO throw a more appropriate Exception here if possible
			throw new RuntimeException("Currently, the simple key store only support byte sized keys. "
					+ "Keys of a bit size that is not a multiple of 8 can not be retrieved.");
		}
		String keyID 			= ce.getKeyStoreID(); // ID of key to get
		int keyLengthInBytes 	= MessageSystem.getCipher().getKeyLength() / 8; // # of key bytes to get
		byte[] decryptionKey 	= SimpleKeyStore.getKeyBytesAtIndexN(keyID, keyLengthInBytes, msg.getMessageArgs().keyIndex());
		return decryptionKey;
	}
	
}
