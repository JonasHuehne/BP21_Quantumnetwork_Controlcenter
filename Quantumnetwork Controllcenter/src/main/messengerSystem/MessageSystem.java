package messengerSystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import encryptionDecryption.AES256;
import encryptionDecryption.CryptoUtility;
import exceptions.NoKeyForContactException;
import exceptions.NoSuchContactException;
import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;
import java.util.Random;

import javax.crypto.SecretKey;

import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things, like signals and prefixes.
 * Send and receiving messages via these methods, the connectionID determines which connectionEndpoint to interact with.
 *
 * @author Jonas Huehne, Sarah Schumann, Lukas Dentler, Sasha Petri
 *
 */
public class MessageSystem {
	
	private static final String ENCODING_STANDARD = "ISO-8859-1";
	
	public static ConnectionManager conMan;
	private static final byte[] DEBUGGING_KEY = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};
	/** When trying to get a key for a contact with this name, instead return {@link #debuggingKey} */
	public static final String DEBUGGING_KEY_CONTACT_NAME = "293402934";
	/** When calling a method that waits to receive a message from a specified connection endpoint, wait this many seconds */
	public static final int RECEIVE_MESSAGE_TIMEOUT = 10;
	
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
	 * receives a signed message
	 * 
	 * @param connectionID the name of the ConnectionEndpoint
	 * @return the received message as byte[], null if error none, on time-out, if the decoding failed or if result of verify was false
	 */
	public static byte[] readAuthenticatedMessage(String connectionID) {
		Instant startWait = Instant.now();
		while(getNumberOfPendingMessages(connectionID) < 1) {
			Instant current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() > RECEIVE_MESSAGE_TIMEOUT) {
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
	 * Wrapper method. Calls {@linkplain #readAuthenticatedMessage(String)} and returns the output as a String converted via {@linkplain #byteArrayToString(byte[])}.
	 * @param connectionID
	 * 		see parameter of the same name in {@linkplain #readAuthenticatedMessage(String)}
	 * @return
	 * 		output of {@link #readAuthenticatedMessage(String)} converted via {@linkplain #byteArrayToString(byte[])}
	 */
	public static String readAuthenticatedMessageAsString(String connectionID) {
		return byteArrayToString(readAuthenticatedMessage(connectionID));
	}

	/**
	 * Sends a signed and encrypted message as a {@link NetworkPackage} with type {@link TransmissionTypeEnum.TRANSMISSION}.
	 * 
	 * @param connectionID 
	 * 		the ID of the receiver, must be a contact in the communication list
	 * @param message 
	 * 		the message to be sent
	 * @return 
	 * 		true if the sending of the message worked, false otherwise
	 * @throws NoKeyForContactException 
	 * 		if no key could be found for the specified contact, thus making encryption impossible
	 */
	public static boolean sendEncryptedMessage(String connectionID, final String message) throws NoKeyForContactException {
		return sendEncryptedMessage(connectionID, TransmissionTypeEnum.TRANSMISSION, "", stringToByteArray(message));	
	}
	
	/**
	 * Sends a signed and encrypted file as a {@link NetworkPackage}, currently with type {@link TransmissionTypeEnum.TRANSMISSION} (temporarily)
	 * 
	 * @param connectionID 
	 * 		the ID of the receiver, must be a contact in the communication list
	 * @param filePath 
	 * 		the precise path to the file that should be sent <br>
	 * 		this method internally uses Files.readAllBytes(filePath) so the file must not be too large
	 * @return true 
	 * 		if the file was encrypted and sent successfully, false otherwise
	 * @throws IOException 
	 * 		if the file at the specified location could not be read
	 * @throws NoKeyForContactException 
	 * 		if no key could be found for the specified contact, thus making encryption impossible
	 */
	public static boolean sendEncryptedFile(String connectionID, Path filePath) throws IOException, NoKeyForContactException {
		// Load the File
		byte[] inputFile = Files.readAllBytes(filePath);
		// Send it as an encrypted message
		return sendEncryptedMessage(connectionID, TransmissionTypeEnum.TRANSMISSION, "", inputFile);
		
		/*
		 * TODO: 
		 * After the merge with the big network rework, implement a new transmission type
		 * DATA_TRANSFER which is used for messages that aren't just simple text messages.
		 * This way a receiver can know if they're receiving a file or a text.
		 * The arguments could specify metadata about the file, e.g. it's name and file extension.
		 * TODO 2:
		 * Make sure no shenanigans are possible through specifying spooky file paths (e.g. writing to system32 or whatever)
		 */		
		
	}
	
	/**
	 * Sends an encrypted message / network package with a custom transmission type.
	 * @param contactID
	 * 		the ID of the receiver, must be a contact in the communication list
	 * @param messageType
	 * 		type of the message, currently only {@link TransmissionTypeEnum.TRANSMISSION} is permitted
	 * @param typeArgument
	 * 		arguments for the type, if needed
	 * @param message
	 * 		the message to encrypt
	 * @return
	 * 		true if encrypt and send was successful, false if otherwise
	 * @throws NoKeyForContactException
	 * 		if no key could be found for the specified contact, thus making encryption impossible
	 */
	private static boolean sendEncryptedMessage(String contactID, TransmissionTypeEnum messageType, String typeArgument, final byte[] message) throws NoKeyForContactException {
		if (messageType != TransmissionTypeEnum.TRANSMISSION) { // TODO future version: if (messageType != TRANSMISSION || messageType != FILE_TRANSFER) 
			throw new IllegalArgumentException("Messages of type " + messageType + " can not be encrypted.");
		}
		SecretKey key = getKeyOrDefault(contactID); // may throw NoKeyForContactException
		byte[] encryptedMessage = AES256.encrypt(message, key);
		return sendAuthenticatedMessage(contactID, messageType, typeArgument, encryptedMessage);
	}

	
	/**
	 * receives a signed message with encrypted text
	 * 
	 * @param connectionID the ID of the sender
	 * @return the received and decrypted message as string, null if error none or if result of verify was false
	 * @throws NoKeyForContactException 
	 */
	public static byte[] readEncryptedMessage(String connectionID) throws NoKeyForContactException {
		byte[] encrypted = readAuthenticatedMessage(connectionID);
		SecretKey key = getKeyOrDefault(connectionID);
		
		//decrypting the message and then returning it
		return AES256.decrypt(encrypted, key);
	}
	
	/**
	 * Wrapper method. Calls {@linkplain #readEncryptedMessage(String)} and converts the output with {@linkplain #byteArrayToString(byte[])}.
	 * @param connectionID
	 * 		{@code connectionID} passed to {@linkplain #readEncryptedMessage(String)}
	 * @return
	 * 		output of {@linkplain #readEncryptedMessage(String)} called with the same {@code connectionID} argument, converted via {@linkplain #byteArrayToString(byte[])}
	 * @throws NoKeyForContactException
	 * 		if no key for the specified contact could be found
	 */
	public static String readEncryptedMessageAsString(String connectionID) throws NoKeyForContactException {
		return byteArrayToString(readEncryptedMessage(connectionID));
	}
	
	/**
	 * 
	 * @param connectionID
	 * @param directory
	 * 		the directory to save the file in, will be created if it does not exist
	 * @param fileName
	 * 		the name to save the file under, may include an extension
	 * @throws NoKeyForContactException
	 * 		if no key for the specified contact could be found
	 * @throws NotDirectoryException
	 * 		if {@code directory} does not specify a directory
	 * @throws FileNotFoundException
	 * 		if {@code directory} does not resolve to an existing directory
	 * @throws IOException
	 * 		other IOExceptions may be thrown if the writing of the file fails
	 */
	public static void receiveAndWriteEncryptedFile(String connectionID, Path directory, String fileName) throws NoKeyForContactException, IOException {
		// TODO once file sending is properly implemented, the signature of this method will need to change (fileName will be removed)
		if(!Files.isDirectory(directory)) throw new NotDirectoryException("Path " + directory.toString() + " does not specify a directory.");
		if(!Files.exists(directory)) throw new FileNotFoundException("Path " + directory.toString() + " did not resolve to an existing directory.");
		
		// Get the key for the contact, used to decrypt the received file
		// TODO: Check whether getEntryFromKeyStore marks the key as used immediately - should not be done here yet
		// TODO: Make sure there are no synchronisation issues with the key (e.g. one party using up bits the other party hasn't used yet - maybe via message args?)
		SecretKey key = getKeyOrDefault(connectionID);

		// Attempt to receive the file
		byte[] fileBytesEnc = readAuthenticatedMessage(connectionID);
		
		// If a file is received, decrypt it and save it
		CryptoUtility.decryptAndSaveFile_AES256(fileBytesEnc, key, directory, fileName);
	}

	/**
	 * Returns the key for a contact, or a default key if {@value #DEBUGGING_KEY_CONTACT_NAME} is given for {@code contactName}. <br>
	 * <b> Currently automatically converts the key, which is saved as a byte array, to an AES256 SecretKey Object. <b>
	 * @param contactName
	 * 		the name of the contact to get a key for <br>
	 * 		may also be {@value #DEBUGGING_KEY_CONTACT_NAME} to receive a default key
	 * @return
	 * 		an 
	 * @throws NoKeyForContactException
	 * @see KeyStoreDbManager
	 */
	private static SecretKey getKeyOrDefault(String contactName) throws NoKeyForContactException {
		/*
		 * TODO: Should later symmetric encryption schemes be used, this method will need to be changed.
		 * Possibly change .byteArrayToSecretKeyAES256(...) to instead be a method that turns a byte
		 * array into the SecretKey object of the currently used cipher / a cipher specified in a parameter.
		 */
		
		byte[] byteKey;
		if(contactName.equals(DEBUGGING_KEY_CONTACT_NAME) ) { // Case 1: Using the Debug Key
			byteKey = DEBUGGING_KEY;
		} else {
			//getting key
			KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(contactName);
			if (keyObject == null) throw new NoKeyForContactException();
			byteKey = keyObject.getKeyBuffer();
		
			KeyStoreDbManager.changeKeyToUsed(contactName);
		}
		
		return CryptoUtility.byteArrayToSecretKeyAES256(byteKey);
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
