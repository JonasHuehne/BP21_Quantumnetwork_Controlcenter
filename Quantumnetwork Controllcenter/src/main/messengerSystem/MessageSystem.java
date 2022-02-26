package messengerSystem;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import encryptionDecryption.AES256;
import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;
import java.util.Random;
import frame.QuantumnetworkControllcenter;
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
	
	private static final String ENCODING_STANDARD = "ISO-8859-1";
	
	public static ConnectionManager conMan;
	private static final byte[] debuggingKey = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};

	/**This simply sends a message on the given ConnectionEndpoint. No confirmation is expected from the recipient.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param message the message to be sent.
	 * @param sig the signature of an authenticated message.
	 */
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, String argument, byte[] message, String sig) {

		//Check if connectionManager exists
		if(conMan == null) {
			System.err.println("WARNING: Tried to send a message via the MessageSystem before initializing the QuantumnetworkControllcenter, thereby setting the connectionManager Reference.");
			return;
		}

		//Check if connectionEndpoint is connected to something.
		ConnectionState state = conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			
			//Send the messages
			conMan.sendMessage(connectionID, type, argument, message, sig);
		}
		else {
			System.err.println("[" + connectionID + "]: Sending of Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
	}
	
	
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, String argument, String message, String sig) {
		sendMessage(connectionID, type, argument, stringToByteArray(message), sig);
	}

	/**Similar to sendMessage but allows for custom prefix. Used for internal system calls via the net.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the signal from.
	 * @param signal used as message prefix, should be one of the cases inside ConnectionEndpoint.java -> processMessage()
	 * @param signalTypeArgument a special string that contains further information that some signalTypes need to work. Can be "" if not needed.
	 */
	public static void sendSignal(String connectionID, TransmissionTypeEnum signal, String signalTypeArgument) {
		
		//Check if connectionManager exists
		if(conMan == null) {
			System.err.println("WARNING: Tried to send a message via the MessageSystem before initializing the QuantumnetworkControllcenter, thereby setting the connectionManager reference.");
			return;
		}

		//Check if connectionEndpoint is connected to something.
		ConnectionState state = conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			
			//Send the signal
			sendMessage(connectionID, signal, signalTypeArgument, (byte[])null, "");
		}
		else {
			System.err.println("[" + connectionID + "]: Sending of message: " + signal + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
	}


	/**This sends a message and the recipient is going to echo the message back to us.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param message the message to be sent.
	 * @param sig optional signature used by authenticated messages.
	 * @return returns True if the confirmation of the message has been received, False if it times out.
	 */
	public static boolean sendConfirmedMessage(String connectionID, byte[] message, String sig) {
		
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
	
	public static boolean sendConfirmedMessage(String connectionID, String message, String sig) {
		return sendConfirmedMessage(connectionID, stringToByteArray(message), sig);
	}
	
	
	/**This generates a random MessageID that can be used to identify a message reception confirmation when using sendConfirmedMessage().
	 * The ID is a 16 alpha-numerical characters long String. (a-z,A-Z,0-9)
	 * @return the new random MessageID
	 */
	public static String generateRandomMessageID() {
		Random randomGen = new Random();
	    return randomGen.ints(48, 123).filter(i -> (i<=57||i>=65) && (i<=90||i>=97)).limit(16).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	/**reads the oldest Message available and removes it from the stack(actually a queue)
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the oldest message
	 */
	public static NetworkPackage readReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).readMessageFromStack();
	}

	/**returns the oldest message, but does not remove it from the queue
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the oldest message that was received and not yet read(removed)
	 */
	public static NetworkPackage previewReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).peekMessageFromStack();
	}

	/**returns the last message that was received but does not remove it from the queue
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the latest message
	 */
	public static NetworkPackage previewLastReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).peekLatestMessageFromStack();
	}

	/**Returns the number of messages that are on the stack and waiting to be read(removed).
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the number of messages.
	 */
	public static int getNumberOfPendingMessages(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).sizeOfMessageStack();
	}

	/**Returns a linkedList of all un-read messages
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the list of unread messages.
	 */
	public static LinkedList<String> getAllReceivedMessages(String connectionID){
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getMessageStack();
	}

	
	/**
	 * sends a signed message, it is the first variant of sendAuthenticatedMessage()
	 * The first variant with 2 parameters sends confirmedMessages, meaning it will wait for a confirmation that the message was received on the other end.
	 * It should be used for standard message- and filetransfers, i.e. transmissions of type TRANSMISSION.
	 * 
	 * The second variant with 4 parameters does not expect a confirmation, but allows for any TransmissionType to be used.
	 * It is intended to be used for signals to the program at the other end of the connection, i.e. anything other than transmissionType TRANSMISSION
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @param message the message to be sent, can be generic byte[] or String
	 * @return true if the sending of both messages worked, false otherwise
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final byte[] message) {
		String signature;
		signature = QuantumnetworkControllcenter.authentication.sign(byteArrayToString(message));
		return sendConfirmedMessage(connectionID, message, signature);
	}
	
	/**
	 * this is a version of sendAuthenticatedMessage() that takes a String instead of a byte[] as a message to sent.
	 * @param connectionID the name of the connection that this should be sent on.
	 * @param message the message as a String
	 * @return returns true if sending was successful, false and potentially logs an UnsupportedEncodingException if it failed.
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final String message) {
		return sendAuthenticatedMessage(connectionID, stringToByteArray(message));
	}
	
	
	/**
	 * sends a signed message, it is the second variant of sendAuthenticatedMessage()
	 * The first variant with 2 parameters sends confirmedMessages, meaning it will wait for a confirmation that the message was received on the other end.
	 * It should be used for standard message- and filetransfers, i.e. transmissions of type TRANSMISSION.
	 * 
	 * The second variant with 4 parameters does not expect a confirmation, but allows for any TransmissionType to be used.
	 * It is intended to be used for signals to the program at the other end of the connection, i.e. anything other than transmissionType TRANSMISSION
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @param type the type of the transmission. For TransmissionType == TRANSMISSION, i.e. content transmissions, use the first variant instead.
	 * @param argument the optional argument, use depends on the chosen TransmissionType. Refer to ConnectionEndpoint.processMessage() for more information.
	 * @param message the actual message to be transmitted. Can be empty. Most transmissions with content will use the first variant of this method.
	 * @return Returns true unless the Encoding from byte[] to String used for the authentication fails. Then it returns false.
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, TransmissionTypeEnum type, String argument, final byte[] message) {
		String signature;
		signature = QuantumnetworkControllcenter.authentication.sign(byteArrayToString(message));
		sendMessage(connectionID, type, argument, message, signature);
		return true;
	}
	
	/**This is the same as the regular version of sendAuthenticatedMessage() with a custom TransmissionType, but allows for the message to be a String instead of a byte[].
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @param type the type of the transmission. For TransmissionType == TRANSMISSION, i.e. content transmissions, use the first variant instead.
	 * @param argument the optional argument, use depends on the chosen TransmissionType. Refer to ConnectionEndpoint.processMessage() for more information.
	 * @param message the actual message to be transmitted. Can be empty. Most transmissions with content will use the first variant of this method.
	 * @return
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, TransmissionTypeEnum type, String argument, final String message) {
		return sendAuthenticatedMessage(connectionID, type, argument, stringToByteArray(message));
	}

	/**
	 * sends a signed message with encrypted text
	 * 
	 * @param connectionID the ID of the receiver
	 * @param message the message to be sent
	 * @return true if the sending of the message worked, false otherwise
	 */
	public static boolean sendEncryptedMessage(String connectionID, final String message) {
		
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = debuggingKey; 
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getKeyBuffer();
		
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
	 * receives a signed message
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the received message as byte[], null if error none, on time-out, if the decoding failed or if result of verify was false
	 */
	public static byte[] readAuthenticatedMessage(String connectionID) {
		Instant startWait = Instant.now();
		while(getNumberOfPendingMessages(connectionID) < 1) {
			Instant current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() > 10) {
				return null;
			}
		}
		NetworkPackage msg = readReceivedMessage(connectionID);
		byte[] message;
		message = msg.getContent();
		String signature = msg.getSignature();
		if(QuantumnetworkControllcenter.authentication.verify(byteArrayToString(message), signature, connectionID)) {
			return message;
		}
		return null;
	}
	
	/**
	 * receives a signed message with encrypted text
	 * 
	 * @param connectionID the ID of the sender
	 * @return the received and decrypted message as string, null if error none or if result of verify was false
	 */
	public static String readEncryptedMessage(String connectionID) {
		byte[] encrypted = readAuthenticatedMessage(connectionID);
		
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = debuggingKey;
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getKeyBuffer();
		
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
			System.err.println("Error: unsupportet Encoding: " + ENCODING_STANDARD + "!");
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
			System.err.println("Error: unsupportet Encoding: " + ENCODING_STANDARD + "!");
			e.printStackTrace();
			return null;
		}
	}

}
