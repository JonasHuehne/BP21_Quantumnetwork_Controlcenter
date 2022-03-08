package messengerSystem;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import encryptionDecryption.AES256;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things, like signals and prefixes.
 * Send and receiving messages via these methods, the connectionID determines which connectionEndpoint to interact with.
 *
 * @author Jonas Huehne, Sarah Schumann
 *
 */
public class MessageSystem {
	
	private static final String ENCODING_STANDARD = Configuration.getProperty("Encoding");
	
	/** Contains the ConnectionEndpoints for which the MessageSystem handles the high-level messaging. <br>
	 * 	Generally, this is set once when initializing the program, however, for automated tests it may be needed to set this multiple times to simulate different users. */
	public static ConnectionManager conMan;
	private static final byte[] DEBUG_KEY = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};

	/**This simply sends a message on the given ConnectionEndpoint. No confirmation is expected from the recipient.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param type the specific type of message. 
	 * @param argument the type-specific argument.
	 * @param message the message to be sent.
	 * @param sig the signature of an authenticated message. May be null for non-authenticated messages.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 * @see TransmissionTypeEnum
	 */
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, String argument, byte[] message, byte[] sig) 
			throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		//Check if connectionManager exists
		if(conMan == null) {
			System.err.println("ERROR - To send a Message via the MessageSystem, the QuantumNetworkControlCenter needs to be initialized first.");
			return;
		}
		//Check if connectionEndpoint is connected to something.
		ConnectionState state = conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			//Send the messages
			conMan.sendMessage(connectionID, type, argument, message, sig);
		} else {
			throw new EndpointIsNotConnectedException(connectionID, "send a message");
		}
	}
	
	/**This simply sends a message on the given ConnectionEndpoint. No confirmation is expected from the recipient.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param type the specific type of message. Look at TransmissionTypeEnum for more information.
	 * @param argument the type-specific argument. Look at TransmissionTypeEnum for more information.
	 * @param message the message to be sent.
	 * @param sig the signature of an authenticated message. May be null for non-authenticated messages.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, String argument, String message, String sig) 
			throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		if(sig == null) {
			sendMessage(connectionID, type, argument, stringToByteArray(message), null);
		}else {
			sendMessage(connectionID, type, argument, stringToByteArray(message), stringToByteArray(sig));
		}
	}

	/**
	 * This returns the actual message contained inside a given NetworkPackage.
	 * @param transmission the NetworkPackage to open.
	 * @return the content as a byte[]. Can be converted to string with MessageSystem.byteArrayToString().
	 */
	public static byte[] readMessage(NetworkPackage transmission) {
		return transmission.getContent();
	}
	
	/**
	 * This returns the actual message contained inside a given NetworkPackage.
	 * @param transmission the NetworkPackage to open.
	 * @return the content as a String.
	 */
	public static String readMessageAsString(NetworkPackage transmission) {
		return byteArrayToString(transmission.getContent());
	}


	/**This sends a message and requests the recipient to send back a confirmation.
	 * A confirmation is an empty message of type {@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION_RESPONSE}.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param message the message to be sent.
	 * @param sig optional signature used by authenticated messages.
	 * @return returns True if the confirmation of the message has been received, False if it times out.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendConfirmedMessage(String connectionID, byte[] message, byte[] sig) 
			throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		
		ConnectionState state = QuantumnetworkControllcenter.conMan.getConnectionState(connectionID);
		
		//Check if ConnectionEndpoint is connected to something
		if(state == ConnectionState.CONNECTED) {
			
			//Send message:
			
			//Get unique msgID, used for identifying the confirmation response
			String msgID = generateRandomMessageID();
			while(QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getConfirmations().contains(msgID)){
				msgID = generateRandomMessageID();
			}

			//Send the actual message with type RECEPTION_CONFIRMATION_REQUEST asking for an answer confirming the messages reception.
			sendMessage(connectionID, TransmissionTypeEnum.RECEPTION_CONFIRMATION_REQUEST, msgID, message, sig);
			
			//Start Timeout-Wait for the confirmation
			boolean waitForConfirmation = true;
			Instant startWait = Instant.now();
			Instant current;
			
			//Wait for confirmation
			while(waitForConfirmation) {
				current = Instant.now();
				
				//Accept response
				if(Duration.between(startWait, current).toSeconds() <= 10 && QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getConfirmations().contains(msgID)) {
					waitForConfirmation = false;
					//System.out.println("[" + connectionID + "]: Message Confirmation received!");
					QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).clearConfirmation(msgID);
					System.out.println("[" + connectionID + "]: Registered confirmation response for messageID " + msgID);
					return true;
				}
				
				//Timeout if waited for more than 10 seconds without a reception confirmation arriving
				else if (Duration.between(startWait, current).toSeconds() > 10) {
					System.err.println("[" + connectionID + "]: Timed-out while waiting for reception-confirmation of messageID:" + msgID + "!");
					waitForConfirmation = false;
					return false;
				}
			}
			return false;
		}
		else {
			System.err.println("[" + connectionID + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
		return false;

	}
	
	/**This sends a message and the recipient is going to echo the message back to us.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param message the message to be sent.
	 * @param sig optional signature used by authenticated messages.
	 * @return returns True if the confirmation of the message has been received, False if it times out.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendConfirmedMessage(String connectionID, String message, String sig) 
			throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		return sendConfirmedMessage(connectionID, stringToByteArray(message), stringToByteArray(sig));
	}
	
	
	/**This generates a random MessageID that can be used to identify a message reception confirmation when using sendConfirmedMessage().
	 * The ID is a 16 alpha-numerical characters long String. (a-z,A-Z,0-9)
	 * @return the new random MessageID
	 */
	public static String generateRandomMessageID() {
		Random randomGen = new Random();
	    return randomGen.ints(48, 123).filter(i -> (i<=57||i>=65) && (i<=90||i>=97)).limit(16).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}


	
	/**Sends a signed and confirmed Message.
	 * Signing requires valid keys.
	 * 
	 * @param connectionID 
	 * 		 the name of the ConnectionEndpoint to send the message from.
	 * @param message the actual message as a byte[].
	 * @return returns true if the message was confirmed to have been received.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final byte[] message) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		// TODO: add check for valid key pair
		byte[] signature;
		signature = QuantumnetworkControllcenter.authentication.sign(message);
		return sendConfirmedMessage(connectionID, message, signature);
	}
	
	/**Sends a signed and confirmed Message.
	 * Signing requires valid keys.
	 * 
	 * @param connectionID  the name of the ConnectionEndpoint to send the message from.
	 * @param message the actual message as a String.
	 * @return returns true if the message was confirmed to have been received.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final String message) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		return sendAuthenticatedMessage(connectionID, stringToByteArray(message));
	}
	
	/**Sends a signed unconfirmed Message.
	 * Signing requires valid keys.
	 * 
	 * @param connectionID  the name of the ConnectionEndpoint to send the message from.
	 * @param type the type of the transmission.
	 * @param argument the optional argument, use depends on the chosen TransmissionType. Refer to ConnectionEndpoint.processMessage() for more information.
	 * @param message the actual message to be transmitted. Can be empty. Most transmissions with content will use the first variant of this method.
	 * @return returns true if the message was confirmed to have been received.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, TransmissionTypeEnum type, String argument, final byte[] message) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		// TODO: add check for valid key pair
		byte[] signature;
		signature = QuantumnetworkControllcenter.authentication.sign(message);
		sendMessage(connectionID, type, argument, message, signature);
		return true;
	}
	
	/**Sends a signed unconfirmed Message.
	 * Signing requires valid keys.
	 * 
	 * @param connectionID  the name of the ConnectionEndpoint to send the message from.
	 * @param type the type of the transmission.
	 * @param argument the optional argument, use depends on the chosen TransmissionType. Refer to ConnectionEndpoint.processMessage() for more information.
	 * @param message the actual message to be transmitted. Can be empty. Most transmissions with content will use the first variant of this method.
	 * @return returns true if the message was confirmed to have been received.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, TransmissionTypeEnum type, String argument, final String message) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		return sendAuthenticatedMessage(connectionID, type, argument, stringToByteArray(message));
	}

	/**
	 * Receives and verifies a signed message.
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @param transmission the NetworkPackage that was received and needs to be verified.
	 * @return the received message as byte[], null if error none, on time-out, if the decoding failed or if result of verify was false
	 */
	public static byte[] readAuthenticatedMessage(String connectionID, NetworkPackage transmission) {
		NetworkPackage msg = transmission;
		byte[] message;
		message = msg.getContent();
		byte[] signature = msg.getSignature();
		if(QuantumnetworkControllcenter.authentication.verify(message, signature, connectionID)) {
			return message;
		}
		return null;
	}
	
	/**
	 * sends a signed message with encrypted text
	 * 
	 * @param connectionID the ID of the receiver
	 * @param message the message to be sent
	 * @return true if the sending of the message worked, false otherwise
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if the {@linkplain ConnectionManager} does not contain a {@linkplain ConnectionEndpoint} with the specified name
	 * @throws EndpointIsNotConnectedException 
	 * 		if the {@linkplain ConnectionEndpoint} specified by {@code connectionID} is not connected to its partner at the moment
	 */
	public static boolean sendEncryptedMessage(String connectionID, final String message) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = DEBUG_KEY; 
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getCompleteKeyBuffer();
		
		//TODO wird sp�ter vermutlich nicht mehr ben�tigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}
		
		/*
		 * TODO 
		 * hier entsteht noch ziemliches durcheinander!
		 * M�glichkeiten das sauberer zu l�sen:
		 * - keys in der DB direct in passender L�nge speichern 
		 * - dem folgend eine m�glichkeit den entsprechenden key als used zu markieren oder besser:
		 * 	 beim erhalten des Keys diret als used markieren
		 */
		
		String encrypted = AES256.encrypt(message, byteKey);
		
		return sendAuthenticatedMessage(connectionID, encrypted);		
	}
	
	/**
	 * receives a signed message with encrypted text
	 * 
	 * @param connectionID the ID of the sender
	 * @return the received and decrypted message as string, null if error none or if result of verify was false
	 */
	public static String readEncryptedMessage(String connectionID, NetworkPackage transmission) {
		byte[] encrypted = readAuthenticatedMessage(connectionID, transmission);
		
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = DEBUG_KEY;
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getCompleteKeyBuffer();
		
		//TODO wird sp�ter vermutlich nicht mehr ben�tigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}
		
		/*
		 * TODO
		 * 
		 * auch hier herrscht noch Durcheinander und Unklarheiten:
		 * -wie genau wird sich auf einen key geeinigt?
		 *  wird das �ber eine vorherige message gel�st 
		 *  oder wird vom KeyStore eine methode implementiert bei der immer der �lteste unused key zur�ckgegeben wird?
		 *  
		 * Wenn sich vorher auf den key geeinigt wird muss noch ein paramete key hinzugef�gt werden!
		 */
		
		//decrypting the message and then returning it
		return AES256.decrypt(MessageSystem.byteArrayToString(encrypted), byteKey);
	}
	
	
	/**Utility for converting a byte[] to a String.
	 * The Network sends messagesPackages with byte[]s as content. This Method is used 
	 * to convert a byte[] to a String.
	 * @param arr the byte[] that should be converted to a String.
	 * @return	the resulting String, may be null if the encoding failed.
	 */
	public static String byteArrayToString(byte[] arr) {
		try {
			return new String(arr, ENCODING_STANDARD);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Error: unsupported Encoding: " + ENCODING_STANDARD + "!");
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**Utility for converting a String to a byte[].
	 * The Network sends messagesPackages with byte[]s as content. This Method is used 
	 * to convert a String to a byte[].
	 * @param str the String that should be converted to a byte[].
	 * @return	the resulting byte[], may be null if the encoding failed.
	 */
	public static byte[] stringToByteArray(String str) {
		try {
			return str.getBytes(ENCODING_STANDARD);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Error: unsupported Encoding: " + ENCODING_STANDARD + "!");
			e.printStackTrace();
			return null;
		}
	}

}
