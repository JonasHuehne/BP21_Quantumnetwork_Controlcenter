package networkConnection;

import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.BadPaddingException;

import exceptions.CouldNotDecryptMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;
import exceptions.VerificationFailedException;
import keyStore.SimpleKeyStore;
import messengerSystem.Authentication;
import messengerSystem.MessageSystem;

/**
 * Low level handling of {@linkplain NetworkPackage}s received by a {@linkplain ConnectionEndpoint}. <br>
 * @implNote Not all handling of {@linkplain NetworkPackage}s can be done here, since some (e.g. connection request confirmations)
 * require setting of private fields, however, this still allows to reduce the bloat of the {@linkplain ConnectionEndpoint} class somewhat.
 * @author Sasha Petri, Jonas Hühne
 */
public class NetworkPackageHandler {

	
	/** Whether files that do not have a valid signature should be saved to the system */
	final static boolean saveUnverifiedFiles = false;
	
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
		case RECEPTION_CONFIRMATION:
			// TODO think if something special needs to be done here
			// maybe just making an .isConfirmed(byte[] id) method that checks the message list for Confirmations
			// for that ID is sufficient to allow outside classes to check if a message was confirmed 
			break;
		case TEXT_MESSAGE:
			handleTextMessage(ce, msg);
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
						// Get the key for decryption
						if (MessageSystem.getCipher().getKeyLength() % 8 != 0) { // check needed, because SimpleKeyStore only supports byte-sized keys
							// TODO throw a more appropriate Exception here if possible
							throw new RuntimeException("Currently, the simple key store only support byte sized keys. "
									+ "Keys of a bit size that is not a multiple of 8 can not be retrieved.");
						}
						String keyID 			= ce.getKeyStoreID(); // ID of key to get
						int keyLengthInBytes 	= MessageSystem.getCipher().getKeyLength() / 8; // # of key bytes to get
						byte[] decryptionKey 	= SimpleKeyStore.getKeyBytesAtIndexN(keyID, keyLengthInBytes, msg.getMessageArgs().keyIndex());
						// Decrypt
						byte[] decryptedMsg 	= MessageSystem.getCipher().decrypt(msg.getContent(), decryptionKey);
						String decryptedString	= MessageSystem.byteArrayToString(decryptedMsg);
						// Log the decrypted text of the message
						ce.appendMessageToChatLog(false, decryptedString);
					} catch (SQLException | InvalidKeyException | BadPaddingException | NoKeyForContactException | NotEnoughKeyLeftException e) {
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
				throw new VerificationFailedException("Could not verify the text message with ID " 
						+ Base64.getEncoder().encodeToString(msg.getID()) + " using the public key of " + ce.getID());
			}
		} else {
			ce.appendMessageToChatLog(false, MessageSystem.byteArrayToString(msg.getContent()));
		}
	}
	
	private static void handleFile(ConnectionEndpoint ce, NetworkPackage msg) {
		if (msg.getSignature() != null) { // if the message is signed, verify it
			Authentication authenticator = MessageSystem.getAuthenticator(); // the auth currently in use by the message system
			if (msg.verify(authenticator, ce.getID())) {
				if (msg.getMessageArgs().keyIndex() != -1) {
					// if it is encrypted, decrypt and save it
				} else { 
					// otherwise just save it
				}
			}
			
		} else {
			if (saveUnverifiedFiles) {
				
			} else {
				// TODO log an error or something here
			}
		}
	}
	
}
