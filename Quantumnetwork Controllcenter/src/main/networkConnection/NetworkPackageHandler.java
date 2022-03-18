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

import communicationList.Contact;
import encryptionDecryption.FileCrypter;
import exceptions.CouldNotDecryptMessageException;
import exceptions.CouldNotGetKeyException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import exceptions.VerificationFailedException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.GenericWarningMessage;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;
import messengerSystem.SignatureAuthentication;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;
import sourceControl.SourceControlApplication;

/**
 * Low level handling of {@linkplain NetworkPackage}s received by a {@linkplain ConnectionEndpoint}. <br>
 * @implNote Not all handling of {@linkplain NetworkPackage}s can be done here, since some (e.g. connection request confirmations)
 * require setting of private fields, however, this still allows to reduce the bloat of the {@linkplain ConnectionEndpoint} class somewhat.
 * @author Sasha Petri, Jonas Hühne
 */
public class NetworkPackageHandler {
	
	static Log nphLogger = new Log("NetworkPackageHandler Logger", LogSensitivity.WARNING);

	
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
		
		// if the received message could be verified
		boolean verified = false;
		// if it is a signed message, check the signature first && msg.getType() != TransmissionTypeEnum.KEYGEN_SYNC_REQUEST
		if (msg.getSignature() != null) {
			if (!msg.verify(MessageSystem.getAuthenticator(), ce.getID())) {
				Contact c = QuantumnetworkControllcenter.communicationList.query(ce.getID());
				ce.appendMessageToChatLog(false, -1, "[Contents Discarded]");
				throw new VerificationFailedException("Could not verify the text message with ID " 
						+ Base64.getEncoder().encodeToString(msg.getID()) + " using the public key of " + ce.getID() + 
						"(" + (c != null ? c.getSignatureKey() : "???")  + ")");
			} else {
				verified = true;
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
			handleFile(ce, msg, verified);
			break;
		case KEYGEN_SOURCE_SIGNAL:
			//This is only used for signaling the source server to start sending photons. 
			SourceControlApplication.writeSignalFile(msg, ce.getID());
			break;
		case KEYGEN_SOURCE_DESTROY:
			//This is used after the Source has received the needed info. It is sent from the Source to the sender.
			//Once received, the sender deletes the CE completely, including the message Log and the GUI Entry.
			QuantumnetworkControllcenter.conMan.destroySourceConnection(ce.getID(), true);
			break;
		case KEYGEN_SYNC_ACCEPT:
			//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncConfirm is added to the regular messagesStack and read by the KeyGenerator.
			System.out.println("Received SyncResponse: Accept!");
			ce.getKeyGen().updateAccRejState(1);
			break;
		case KEYGEN_SYNC_REJECT:
			//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncReject is added to the regular messagesStack and read by the KeyGenerator.
			System.out.println("Received SyncResponse: Reject!");
			ce.getKeyGen().updateAccRejState(-1);
			break;
		case KEYGEN_SYNC_REQUEST:
			System.out.println("Received SyncRequest!");
			ce.getKeyGen().keyGenSyncResponse(msg);
			break;
		case KEYGEN_TERMINATION:
			//This is received if the connected ConnectionEndpoint intends to terminate the KeyGen Process. This will cause a local shutdown in response.
			//Terminating Key Gen
			new GenericWarningMessage("The Key Generation Process was terminated by the other party!");
			ce.getKeyGen().shutdownKeyGen(false, true);
			break;
		case KEYGEN_TRANSMISSION:
			ce.getKeyGen().writeKeyGenFile(msg);
			break;
		case RECEPTION_CONFIRMATION: // content of messages of this type is the ID they are confirming
			// TODO check if we actually sent out a message with that ID (this will need us to also log which messages we sent...)
			// Ignore RECEPTION_CONFIRMATIONS that are not verified (confirmation is not useful unless authentic)
			
			if (msg.getSignature() == null) { 
				nphLogger.logWarning("[" + ce.getID() + "] Received confirmation for message with ID " + msg.getStringID()
						+ " but discarded it. The confirmation was not signed.");
				break;
			}
			nphLogger.logInfo("[" + ce.getID() + "] Received confirmation for message with ID " + msg.getStringID());
			// if we did, add the messageID to the list
			ce.addConfirmationFor(msg.getContent());
			// if we have been waiting for this ID to be confirmed before pushing a message
			// (e.g. in-order transfer, key synchronization, ...) push the waiting message now
			NetworkPackage toPush = ce.removeFromPushQueue(msg.getContent());
			if (toPush != null) ce.pushMessage(toPush);
			break;
		case TEXT_MESSAGE:
			handleTextMessage(ce, msg, verified);
			break;
		case KEY_USE_ALERT:
			int encStartIndex = msg.getMessageArgs().keyIndex(); // index at which sender wants to start encrypting
			try {
				int ownIndex = KeyStoreDbManager.getIndex(ce.getKeyStoreID());
				// check if this >= our own index
				if (encStartIndex >= ownIndex) {
					// if it is, everything is good (we set our index to A's index after encryption)
					int incrementAmount = (encStartIndex + MessageSystem.getCipher().getKeyLength() / 8) - ownIndex;
					KeyStoreDbManager.incrementIndex(ce.getKeyStoreID(), incrementAmount);
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
			} catch (NoKeyWithThatIDException e) {
				// Control flow wise, this should not occur - a KEY_USE_ALERT should only be able to be sent if there is a mutual key
				nphLogger.logWarning("[CE " + ce.getID() + " ] Could not process key use alert. "
						+ "There is no entry in the keystore with ID " + ce.getKeyStoreID() + ".", e);
			} catch (SQLException e) {
				// Nothing we can really do here except log it, possible expansion would be a special message type to the partner
				nphLogger.logWarning("[CE " + ce.getID() + " ] Could not process key use alert due to an issue with the key store.", e);
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
				nphLogger.logInfo("[CE " + ce.getID() + " ] Received a message of type " + TransmissionTypeEnum.KEY_USE_REJECT 
								+ " rejecting the package with ID " + msg.getStringID() + ". However, there was no such package in the queue.");
			} else {
				try {
					// Because we never sent our message, we can actually mark the bits used for encryption
					// as unused again, and then set the index to max(k, i), where k is the key index before
					// we tried encrypting, and i is the index given to us by our partner (the local key index of our partner)
					// that way, next time we will be starting at an index that is >= our partners (i.e. unused bytes)
					int k = rejectedPackage.getMessageArgs().keyIndex();
					int i = msg.getMessageArgs().keyIndex();
					int newIndex = Math.max(k, i);
					nphLogger.logInfo("[CE " + ce.getID() + " ] Received a message of type " + TransmissionTypeEnum.KEY_USE_REJECT 
							+ " rejecting the package with ID " + msg.getStringID() + ". Adjusting local key index to be " + newIndex + ".");
					KeyStoreDbManager.changeIndex(ce.getKeyStoreID(), newIndex);
				} catch (NoKeyWithThatIDException e) {
					// Control flow wise, this should not occur - a KEY_USE_ALERT should only be able to be sent if there is a mutual key
					nphLogger.logWarning("[CE " + ce.getID() + " ] Could not adjust key index based on key use rejection message.", e);
				} catch (SQLException e) {
					// Nothing we can really do here except log it, possible expansion would be a special message type to the partner
					nphLogger.logWarning("[CE " + ce.getID() + " ] Could not adjust key index based on key use rejection message.", e);
				} catch (NotEnoughKeyLeftException e) {
					// This shouldn't happen unless we were either out-of-bounds already, or B sent us a non-sensical index
					nphLogger.logWarning("[CE " + ce.getID() + " ] Could not adjust key index based on key use rejection message.", e);
				}
			}
			break;
		default:
			nphLogger.logWarning("[CE " + ce.getID() + " ] A message of type " + msg.getType() + " was received. "
					+ "No handling is defined for this type. Message ID: " + msg.getStringID());
			break;
		}
		
		// Finally, if message could be properly handled (no Exceptions etc.)
		// confirm it, if it was requested. We sign the confirmation message
		// to prove that we (the intended recipient) are actually confirming it.
		
		if (msg.expectedToBeConfirmed()) {
			NetworkPackage confirmation = new NetworkPackage(TransmissionTypeEnum.RECEPTION_CONFIRMATION, new MessageArgs(), msg.getID(), false);
			confirmation.sign(MessageSystem.getAuthenticator());
			try {
				ce.pushMessage(confirmation);
				nphLogger.logInfo(("[CE " + ce.getID() + "] Sent confirmation for message with ID "  + msg.getStringID()));
			} catch (EndpointIsNotConnectedException e) {
				// Log it if no confirmation could be sent, but otherwise continue processing the message as normal
				nphLogger.logError("[CE " + ce.getID() + "] Could not confirm message with ID " +  msg.getStringID(), e);
			}
		}

	}
	
	/*
	 * The two methods below could theoretically go into MessageSystem with some refactoring?
	 */
	
	/**
	 * Handles a text message that was received
	 * @param ce
	 * 		the ConnectionEndpoint that received the message
	 * @param msg
	 * 		the message received - content will be interpreted as a string
	 * @param verified
	 * 		if the message has a valid signature
	 * @throws CouldNotDecryptMessageException
	 * 		if the message was encrypted (keyIndex >= 0) but could not be decrypted
	 * @throws VerificationFailedException
	 * 		if the message was signed (msg.getSignature() != null) but could not be verified
	 */
	private static void handleTextMessage(ConnectionEndpoint ce, NetworkPackage msg, boolean verified) throws CouldNotDecryptMessageException, VerificationFailedException {
		if (msg.getSignature() != null) { // if the message is signed, verify it
			if (verified) {
				// If the message is also encrypted, try to decrypt it
				if (msg.getMessageArgs().keyIndex() != -1) {
					try {
						byte[] decryptionKey = getKey(ce, msg);
						// Decrypt
						byte[] decryptedMsg 	= MessageSystem.getCipher().decrypt(msg.getContent(), decryptionKey);
						String decryptedString	= MessageSystem.byteArrayToString(decryptedMsg);
						// Log the decrypted text of the message
						ce.appendMessageToChatLog(false, 1, decryptedString);
					} catch (InvalidKeyException | BadPaddingException | CouldNotGetKeyException e) {
						throw new CouldNotDecryptMessageException("[CE " + ce.getID() + "] Could not decrypt the text message with ID " + msg.getStringID(), e);
					}
				} 
				// If the message is not encrypted
				else { 
					// It has already been verified, so we just log it
					ce.appendMessageToChatLog(false, 1, MessageSystem.byteArrayToString(msg.getContent()));
				}
			} else {
				ce.appendMessageToChatLog(false, -1, "[Contents Discarded]");
				throw new VerificationFailedException("[CE " + ce.getID() + "] Could not verify the message with ID " + msg.getStringID());
			}
		} else {
			ce.appendMessageToChatLog(false, 0, MessageSystem.byteArrayToString(msg.getContent()));
		}
	}
	
	/**
	 * Handles a file that was received.
	 * @param ce
	 * 		the ConnectionEndpoint that received the message
	 * @param msg
	 * 		the message received - content will be interpreted as the bytes of a file <br>
	 * 		filename will be taken from the message arguments
	 * @param verified
	 * 		if the message has a valid signature
	 * @throws CouldNotDecryptMessageException
	 * 		if the file was encrypted (keyIndex >= 0) but could not be decrypted
	 */
	private static void handleFile(ConnectionEndpoint ce, NetworkPackage msg, boolean verified) throws CouldNotDecryptMessageException {
		// Save the file if saving unverified files is true, or if it can be verified
		Path outDirectory = Paths.get("");
		String fileName = "";
		if (saveUnverifiedFiles || (msg.getSignature() != null && verified)) {
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
						Path pathToDecryptedFile = Paths.get(f.getParent().toString(), f.getName() + "_decrypted"); 
						FileCrypter.decryptAndSave(outDirectory.resolve(fileName).toFile(), MessageSystem.getCipher(), sk, pathToDecryptedFile);
					} catch (BadPaddingException | InvalidKeyException | IOException | CouldNotGetKeyException e) {
						throw new CouldNotDecryptMessageException("Could not decrypt the text message with ID " + Base64.getEncoder().encodeToString(msg.getID()), e);
					}
				} 
			} catch (IOException e) {
				nphLogger.logError("[CE " + ce.getID() + "] An I/O Exception occurred trying to receive the file " + msg.getMessageArgs().fileName(), e);
				return;
			}
		} else {
			nphLogger.logInfo("[CE " + ce.getID() + " ] Saving unverified files is disabled, and the message with ID " 
					+ msg.getStringID() + " did not have a valid signature. So, no file was saved.");
		}

	}
	
	/**
	 * Gets the key to be used to decrypt a passed message.
	 * Looks in the keystore for the key with {@code keyID = ce.getKeystoreId()}
	 * and returns the bytes key[keyIndex] to key[keyIndex + n] of it,
	 * where n is the key length (in bytes) for the currently used cipher.
	 * keyIndex is as specified in the message arguments.
	 * @param ce
	 * 		the ConnectionEndpoint that received the message
	 * @param msg
	 * 		the encrypted message
	 * @return
	 * 		the key that can be used to decrypt the message
	 * @throws CouldNotGetKeyException 
	 * 		if no key could be retrieved for decryption, generally wraps a lower level exception
	 */
	private static byte[] getKey(ConnectionEndpoint ce, NetworkPackage msg) throws CouldNotGetKeyException {
		// Get the key for decryption
		try {
			if (MessageSystem.getCipher().getKeyLength() % 8 != 0) { // check needed, because SimpleKeyStore only supports byte-sized keys
				throw new CouldNotGetKeyException("Currently, the simple key store only support byte sized keys. "
						+ "Keys of a bit size that is not a multiple of 8 can not be retrieved.", null);
			}
			String keyID 			= ce.getKeyStoreID(); // ID of key to get
			int keyLengthInBytes 	= MessageSystem.getCipher().getKeyLength() / 8; // # of key bytes to get
			byte[] decryptionKey 	= KeyStoreDbManager.getKeyBytesAtIndexN(keyID, keyLengthInBytes, msg.getMessageArgs().keyIndex());
			return decryptionKey;
		} catch (SQLException | NotEnoughKeyLeftException | NoKeyWithThatIDException e) {
			throw new CouldNotGetKeyException("[CE " + ce.getID() + "] Could not get key to decrypt message with ID " + msg.getStringID(), e);
		}
		

	}
	
}
