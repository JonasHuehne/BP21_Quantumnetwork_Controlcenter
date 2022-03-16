package networkConnection;

import encryptionDecryption.SymmetricCipher;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import messengerSystem.Authentication;
import messengerSystem.MessageSystem;

/**
 * Low level handling of {@linkplain NetworkPackage}s received by a {@linkplain ConnectionEndpoint}. <br>
 * @implNote Not all handling of {@linkplain NetworkPackage}s can be done here, since some (e.g. connection request confirmations)
 * require setting of private fields, however, this still allows to reduce the bloat of the {@linkplain ConnectionEndpoint} class somewhat.
 * @author Sasha Petri, Jonas Hühne
 */
public class NetworkPackageHandler {

	private static Authentication authenticator;
	private static SymmetricCipher cipher;
	
	/** Whether files that do not have a valid signature should be saved to the system */
	final static boolean saveUnverifiedFiles = false;
	
	public static void setAuthenticator(Authentication auth) {
		NetworkPackageHandler.authenticator = auth;
	}
	
	public static void setCipher(SymmetricCipher cipher) {
		NetworkPackageHandler.cipher = cipher;
	}
	
	/**
	 * Processes a NetworkPackage.
	 * @param ce
	 * 		the ConnectionEndpoint that received the package
	 * @param msg
	 * 		the network package to process
	 * @throws EndpointIsNotConnectedException 
	 * 		can be thrown during key generation <br>
	 * 		when processing a message would lead to sending a message back, but this endpoint is not connected to their partner
	 */
	public static void handlePackage(ConnectionEndpoint ce, NetworkPackage msg) throws EndpointIsNotConnectedException {
		
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
	
	private static void handleTextMessage(ConnectionEndpoint ce, NetworkPackage msg) {
		if (msg.getSignature() != null) { // if the message is signed, verify it
			if (msg.verify(authenticator, ce.getID())) {
				if (msg.getMessageArgs().keyIndex() != -1) {
					// if it is encrypted, decrypt it
					// add the decrypted text to the chat log
				} else { 
					// otherwise just chat log it	
					ce.appendMessageToChatLog(false, MessageSystem.byteArrayToString(msg.getContent()));
				}
			}
		}
		ce.appendMessageToChatLog(false, MessageSystem.byteArrayToString(msg.getContent()));
	}
	
	private static void handleFile(ConnectionEndpoint ce, NetworkPackage msg) {
		if (msg.getSignature() != null) { // if the message is signed, verify it
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
