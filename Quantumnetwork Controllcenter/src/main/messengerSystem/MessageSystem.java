package messengerSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * @author Jonas Huehne, Sarah Schumann, Lukas Dentler
 *
 */
public class MessageSystem {
	
	public static ConnectionManager conMan;
	private static final byte[] debuggingKey = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};

	/**This simply sends a message on the given ConnectionEndpoint. No confirmation is expected from the recipient.
	 *
	 * @param connectionID the name of the ConnectionEndpoint to send the message from.
	 * @param message the message to be sent.
	 * @param sig the signature of an authenticated message.
	 */
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, String argument, String message, String sig) {

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
			conMan.sendMessage(connectionID, signal, signalTypeArgument, "", "");
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
	public static boolean sendConfirmedMessage(String connectionID, String message, String sig) {
		
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
			conMan.sendMessage(connectionID, TransmissionTypeEnum.RECEPTION_CONFIRMATION_REQUEST, msgID, message, sig);
			
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
	
	/**This generates a random MessageID that can be used to identify a message reception confirmation when using sendConfirmedMessage().
	 * The ID is a 16 alpha-numerical characters long String. (a-z,A-Z,0-9)
	 * @return the new random MessageID
	 */
	private static String generateRandomMessageID() {
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
	 * @param message the message to be sent
	 * @return true if the sending of both messages worked, false otherwise
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final String message) {
		String signature = QuantumnetworkControllcenter.authentication.sign(message);
		return sendConfirmedMessage(connectionID, message, signature);

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
	 * @param type	the type of the transmission. For TransmissionType == TRANSMISSION, i.e. content transmissions, use the first variant instead.
	 * @param argument	the optional argument, use depends on the chosen TransmissionType. Refer to ConnectionEndpoint.processMessage() for more information.
	 * @param message	the actual message to be transmitted. Can be empty. Most transmissions with content will use the first variant of this method.
	 * @return Always returns true, because it does not expect a confirmation.
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, TransmissionTypeEnum type, String argument, final String message) {
		String signature = QuantumnetworkControllcenter.authentication.sign(message);
		sendMessage(connectionID, type, argument, message, signature);
		return true;
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
		
		//TODO wird spï¿½ter vermutlich nicht mehr benï¿½tigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}
		
		/*
		 * TODO 
		 * hier entsteht noch ziemliches durcheinander!
		 * Mï¿½glichkeiten das sauberer zu lï¿½sen:
		 * - keys in der DB direct in passender Lï¿½nge speichern 
		 * - dem folgend eine mï¿½glichkeit den entsprechenden key als used zu markieren oder besser:
		 * 	 beim erhalten des Keys diret als used markieren
		 */
		
		String encrypted = AES256.encrypt(message, byteKey);
		
		return sendAuthenticatedMessage(connectionID, encrypted);		
	}
	
	/**
	 * receives a signed message
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the received message as string, null if error none, on time-out or if result of verify was false
	 */
	public static String readAuthenticatedMessage(String connectionID) {
		Instant startWait = Instant.now();
		while(getNumberOfPendingMessages(connectionID) < 1) {
			Instant current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() > 10) {
				return null;
			}
		}
		NetworkPackage msg = readReceivedMessage(connectionID);
		String message = msg.getContent();
		String signature = msg.getSignature();
		if(QuantumnetworkControllcenter.authentication.verify(message, signature, connectionID)) {
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
		String encrypted = readAuthenticatedMessage(connectionID);
		
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = debuggingKey;
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getKeyBuffer();
		
		//TODO wird spï¿½ter vermutlich nicht mehr benï¿½tigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}
		
		/*
		 * TODO
		 * 
		 * auch hier herrscht noch Durcheinander und Unklarheiten:
		 * -wie genau wird sich auf einen key geeinigt?
		 *  wird das ï¿½ber eine vorherige message gelï¿½st 
		 *  oder wird vom KeyStore eine methode implementiert bei der immer der ï¿½lteste unused key zurï¿½ckgegeben wird?
		 *  
		 * Wenn sich vorher auf den key geeinigt wird muss noch ein paramete key hinzugefï¿½gt werden!
		 */
		
		//decrypting the message and then returning it
		return AES256.decrypt(encrypted, byteKey);
	}
	
	public static boolean sendFile(String connectionID, byte[] fileData) {
		//dummy method until real one is finished
		return true;
	}
	
	public static byte[] receiveFile(String connectionID) {
		//dummy method until real one is finished#
		return null;
	}
	
	/**
	 * helper method to identify the current path to the messageSystem directory
	 * 
	 * @return Path of the messageSystem directory, returns null if messageSystem directory does not exist
	 */
	private static Path getMessageSystemPath() {
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
		Path messageSystemPath = currentWorkingDir.resolve("messageSystem");
		if(!Files.isDirectory(messageSystemPath)) {
			System.err.println("Error, could not find the externalAPI folder, expected: " + messageSystemPath.normalize());
			return null;
		}
		return messageSystemPath;
	}
	
	/**
	 * encrypts and sends the given file to the specified receiver
	 * 
	 * @param connectionID the ID of the receiver
	 * @param filePath the precise Path to the File that should be sent
	 * @return true if the file was encrypted and sent successfully, false otherwise
	 */
	public static boolean sendEncryptedFile(String connectionID, Path filePath) {
		//getting directory of connection partner
		Path messageSystemDir = getMessageSystemPath();
		Path connectionDir = messageSystemDir.resolve(connectionID);
		//creating directory if it does not exist
		if(!Files.isDirectory(connectionDir)) {
			try {
				Files.createDirectory(connectionDir);
			} catch (IOException e) {
				System.err.println(e.toString());
				return false;
			}
		}
		
		//getting key for encryption
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = debuggingKey;
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getKeyBuffer();
		
		//TODO wird später vermutlich nicht mehr benötigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}

		byte[] byteArrayFile = AES256.encryptFileToByteArray(filePath.toFile(), byteKey);
		
		if(byteArrayFile == null) {
			return false;
		}
		
		String fileName = filePath.toFile().getName();
		
		boolean sentFileName = sendAuthenticatedMessage(connectionID, fileName);
		boolean sentFile = sendFile(connectionID, byteArrayFile);
		
		return sentFileName && sentFile;
	}
	
	/**
	 * receives and decrypts a file sent by the specified connectionID and saves it to the given directory path
	 * 
	 * @param connectionID the ID of the sender
	 * @param pathName the path to the directory where the file should be saved
	 */
	public static void receiveEncryptedFileToPath(String connectionID, Path pathName) {
		//getting key for encryption
		byte[] byteKey;
		if(connectionID.equals("42debugging42") || connectionID.equals("41debugging41") ) {
			byteKey = debuggingKey;
		}
		else {
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byteKey = keyObject.getKeyBuffer();
		
		//TODO wird später vermutlich nicht mehr benötigt
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		}
		
		String fileName = readAuthenticatedMessage(connectionID);
		File decrypted = pathName.resolve("decrypted_" + connectionID + "_" + fileName).toFile();
		
		byte[] encrypted = receiveFile(connectionID);
		
		AES256.decryptByteArrayToFile(encrypted, byteKey, decrypted);
	}
	
	/**
	 * receives and decrypts a file sent by the specified connectionID and saves it to the directory corresponding with the connectionID
	 * 
	 * @param connectionID the ID of the sender
	 */
	public static void receiveEncryptedFile(String connectionID) {
		//getting directory of connection partner
		Path messageSystemDir = getMessageSystemPath();
		Path connectionDir = messageSystemDir.resolve(connectionID);
		
		//creating directory if it does not exist
		if(!Files.isDirectory(connectionDir)) {
			try {
				Files.createDirectory(connectionDir);
			} catch (IOException e) {
				System.err.println(e.toString());
			}
		}
		
		receiveEncryptedFileToPath(connectionID, connectionDir);
	}

}
