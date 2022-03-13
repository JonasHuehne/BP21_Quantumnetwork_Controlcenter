package messengerSystem;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Random;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import encryptionDecryption.FileCrypter;
import encryptionDecryption.SymmetricCipher;
import exceptions.CouldNotSendMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;
import keyStore.SimpleKeyStore;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.MessageArgs;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;
import qnccLogger.Log;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things, like signals and prefixes.
 * Send and receiving messages via these methods, the connectionID determines which connectionEndpoint to interact with.
 *
 * @author Jonas Huehne, Sarah Schumann, Sasha Petri
 *
 */
public class MessageSystem {
	
	static Log messageSystemLog = new Log("MessageSystem Log");
	
	/** The cipher the message system uses to encrypt / decrypt messages & files */
	private static SymmetricCipher cipher;
	/** The authenticator the message system uses to sign / verify messages & files */
	private static Authentication authenticator;
	
	private static final String ENCODING_STANDARD = Configuration.getProperty("Encoding");
	
	/** Contains the ConnectionEndpoints for which the MessageSystem handles the high-level messaging. <br>
	 * 	Generally, this is set once when initializing the program, however, for automated tests it may be needed to set this multiple times to simulate different users. */
	public static ConnectionManager conMan;
	private static final byte[] DEBUGKEY = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};
	
	/**
	 * Sets the encryption / decryption algorithm to be used by the MessageSystem.
	 * @param cipher
	 * 		the algorithm to use
	 */
	public static void setEncryption(SymmetricCipher cipher) {
		MessageSystem.cipher = cipher;
	}

	/**
	 * Sets the authentication algorithm to be used by the MessageSystem.
	 * @param authentication
	 * 		the algorithm to use
	 */
	public static void setAuthenticationAlgorithm(Authentication authentication) {
		MessageSystem.authenticator = authentication;
	}
	
	/**
	 * @return the message authenticator currently in use by the MessageSystem
	 */
	public static Authentication getAuthenticator() {
		return authenticator;
	}
	
	/**
	 * @return the symmetric encryption algorithm used in this class when encryption messages
	 */
	public static SymmetricCipher getCipher() {
		return cipher;
	}
	
	/**
	 * Constructs a NetworkPackage with the given parameters and sends it to the specified partner. <br>
	 * Signs the NetworkPackage with the authenticator of this class if desired.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the constructed NetworkPackage is sent to the connected partner of the specified endpoint
	 * @param type
	 * 		type of the NetworkPackage
	 * @param args
	 * 		arguments of the NetworkPackage
	 * @param content
	 * 		content of the network package
	 * @param sign
	 * 		whether to sign the NetworkPackage
	 * @param confirm
	 * 		whether the recipient should confirm that they received the package
	 * @throws EndpointIsNotConnectedException
	 * 		if the specified endpoint is not connected to their partner
	 * @throws ManagerHasNoSuchEndpointException
	 * 		if no endpoint of the given name exists in the ConnectionManager of this class
	 */
	private static void sendMessage(String connectionID, TransmissionTypeEnum type, MessageArgs args, byte[] content, boolean sign, boolean confirm)
			throws EndpointIsNotConnectedException, ManagerHasNoSuchEndpointException {
		NetworkPackage message = new NetworkPackage(type, args, content, confirm);
		if (sign) message.sign(authenticator);
		conMan.sendMessage(connectionID, message);
	}
	
	/**
	 * 
	 * @param connectionID
	 * @param file
	 * @param encryptFile
	 * @param sign
	 * @param confirm
	 * @throws CouldNotSendMessageException
	 * 		if the file could not be sent <br>
	 * 		wraps a lower level exception, such as IOException or {@linkplain EndpointIsNotConnectedException}
	 */
	private static void sendFileInternal(String connectionID, File file, boolean encryptFile, boolean sign, boolean confirm) throws CouldNotSendMessageException {
		byte[] fileBytes;
		int keyIndex;
		
		try {
			if (encryptFile) { // sending an encrypted file
				// encrypt the file locally
				byte[] byteKey = SimpleKeyStore.getNextKeyBytes(connectionID, cipher.getKeyLength() / 8);
				keyIndex = SimpleKeyStore.getIndex(connectionID);
				SecretKey key = cipher.byteArrayToSecretKey(byteKey);
				Path pathToEncryptedFile = Paths.get(file.getParent().toString(), "encrypted_" + file.getName());
				FileCrypter.encryptAndSave(file, cipher, key, pathToEncryptedFile);
				// read the encrypted file to send
				fileBytes = Files.readAllBytes(pathToEncryptedFile);
			} else {
				// read the file to send
				fileBytes = Files.readAllBytes(file.toPath());
				keyIndex = -1; // indicates file isn't encrypted
			}
			
			/*
			 * For large files, it may (?) be beneficial to split it into multiple packages. 
			 * However, this would be a significantly more complicated protocol to prevent package loss etc.
			 */
			
			MessageArgs args = new MessageArgs(file.getName(), keyIndex);
			NetworkPackage msg = new NetworkPackage(TransmissionTypeEnum.FILE_TRANSFER, args, fileBytes, confirm);
			if (sign) msg.sign(authenticator);
			if (encryptFile) {
				informAndSendOnceConfirmed(connectionID, msg);
			} else {
				conMan.sendMessage(connectionID, msg);
			}
			
			
		} catch (EndpointIsNotConnectedException | IOException | InvalidKeyException | 
				IllegalBlockSizeException | SQLException | NotEnoughKeyLeftException | 
				NoKeyForContactException | SecurityException | ManagerHasNoSuchEndpointException e) {
			throw new CouldNotSendMessageException("Could not send the file " + file.getName() + " along the connection " + connectionID + ".", e);
		}

	}
	
	/**
	 * Sends an unencrypted text message to the specified communication partner.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the text message is sent to the connected partner of the specified endpoint
	 * @param msgString
	 * 		the message to send
	 * @param sign
	 * 		true if the message should be signed, false if not
	 * @param confirm
	 * 		set this to true if the recipient should send a confirmation message upon receiving the message, false otherwise
	 * @throws CouldNotSendMessageException
	 * 		if the message could not be sent <br>
	 * 		wraps a lower level exception, such as IOException or {@linkplain EndpointIsNotConnectedException}
	 */
	public static void sendTextMessage(String connectionID, String msgString, boolean sign, boolean confirm) throws CouldNotSendMessageException  {
		MessageArgs args = new MessageArgs();
		messageSystemLog.logInfo("Attempting to send message <" + msgString + "> from CE with ID <" + connectionID + "> | Signed: " + sign + " Confirmed: " + confirm + " |");
		try {
			sendMessage(connectionID, TransmissionTypeEnum.TEXT_MESSAGE, args, stringToByteArray(msgString), sign, confirm);
		} catch (EndpointIsNotConnectedException | ManagerHasNoSuchEndpointException e) {
			throw new CouldNotSendMessageException("Could not send the specified message along the connection " + connectionID + ".", e);
		}
	}
	
	/**
	 * Encrypts a text message and sends it to the specified communication partner.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the text message is sent to the connected partner of the specified endpoint
	 * @param msgString
	 * @param confirm
	 * 		set this to true if the recipient should send a confirmation message upon receiving the message, false otherwise
	 * @throws CouldNotSendMessageException
	 * 		if the message could not be sent, or an error during encryption occured <br>
	 * 		wraps a lower level exception, such as IOException or {@linkplain EndpointIsNotConnectedException}
	 */
	public static void sendEncryptedTextMessage(String connectionID, String msgString, boolean confirm) throws CouldNotSendMessageException {
		try {
			// encrypt the message
			String keyIDofConnection = conMan.getConnectionEndpoint(connectionID).getKeyStoreID();
			byte[] msgBytes = stringToByteArray(msgString);
			byte[] key = SimpleKeyStore.getNextKeyBytes(keyIDofConnection, cipher.getKeyLength() / 8);
			byte[] encMsgBytes = cipher.encrypt(msgBytes, key);
			
			// Provide the index in the message args so receiver knows where to start with decryption
			int index = SimpleKeyStore.getIndex(keyIDofConnection);
			
			messageSystemLog.logInfo("Attempting to send encrypted <" + msgString + "> from CE with ID <" + connectionID + "> | "
					+ "Started Encryption at Index: " + index + " Confirmed: " + confirm + " |");
				
			// Construct the message to send
			MessageArgs args = new MessageArgs(index);
			NetworkPackage msg = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, args, encMsgBytes, confirm);
			msg.sign(authenticator);
			// Tell the other party we wish to send, and queue the message
			informAndSendOnceConfirmed(connectionID, msg);
		} catch (EndpointIsNotConnectedException | SQLException | NotEnoughKeyLeftException 
				| NoKeyForContactException | InvalidKeyException | IllegalBlockSizeException 
				| SecurityException e) {
			throw new CouldNotSendMessageException("Could not send the encrypted message along the connection " + connectionID + ".", e);
		}
	}
	
	/**
	 * Sends an unencrypted file to the specified communication partner.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the file is sent to the connected partner of the specified endpoint
	 * @param file
	 * 		the file to send
	 * @param sign
	 * 		true if the message should be signed, false if not
	 * @param confirm
	 * 		set this to true if the recipient should send a confirmation message upon receiving the file, false otherwise
	 * @throws CouldNotSendMessageException
	 * 		if the file could not be sent <br>
	 * 		wraps a lower level exception, such as IOException or {@linkplain EndpointIsNotConnectedException} 
	 */
	public static void sendFile(String connectionID, File file, boolean sign, boolean confirm) throws CouldNotSendMessageException {
		sendFileInternal(connectionID, file, false, sign, confirm);
	}
	
	/**
	 * Encrypts a file and sends it to the specified communication partner.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the file is sent to the connected partner of the specified endpoint
	 * @param file
	 * 		the file to encrypt and send
	 * @param confirm
	 * 		set this to true if the recipient should send a confirmation message upon receiving the file, false otherwise
	 * @throws CouldNotSendMessageException
	 * 		if the file could not be sent, or an error during encryption occured <br>
	 * 		wraps a lower level exception, such as IOException or {@linkplain EndpointIsNotConnectedException} 
	 */
	public static void sendEncryptedFile(String connectionID, File file, boolean confirm) throws CouldNotSendMessageException {
		sendFileInternal(connectionID, file, true, true, confirm);
	}
	
	private static void informAndSendOnceConfirmed(String connectionID, NetworkPackage msg) 
			throws NoKeyForContactException, SQLException, EndpointIsNotConnectedException {
		
		/*
		 * (Proof of concept)
		 */

		// CE A will send this package to CE B to inform them that
		// A wishes to use bytes of their mutual key, starting at the specified index
		final ConnectionEndpoint ceA = conMan.getConnectionEndpoint(connectionID);
		int currentIndex = SimpleKeyStore.getIndex(ceA.getKeyStoreID());
		final MessageArgs args = new MessageArgs(currentIndex);
		final NetworkPackage keyUseAlert = new NetworkPackage(TransmissionTypeEnum.KEY_USE_ALERT, args, false);
		ceA.pushOnceConfirmationReceivedForID(keyUseAlert.getID(), msg); // push the main message once key use is confirmed
		ceA.pushMessage(keyUseAlert);
		
		// After sending this, increment the index because we used those bits now
		SimpleKeyStore.incrementIndex(connectionID, getCipher().getKeyLength() / 8);
		
		/*
		 * TODO: Asynchronously wait 3 seconds, if no confirmation arrives, delete the package from the queue.
		 */
		
		// Could be better to have a thread that runs for a few seconds and sends multiple messages
		// requesting approval, to account for package loss to mitigate the two generals problem
		// however, as far as I know the transfer via the Sockets is already TCP so that might be redundant
		
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

	/**This generates a random MessageID that can be used to identify a message reception confirmation when using sendConfirmedMessage().
	 * The ID is a 16 alpha-numerical characters long String. (a-z,A-Z,0-9)
	 * @return the new random MessageID
	 */
	public static String generateRandomMessageID() {
		Random randomGen = new Random();
	    return randomGen.ints(48, 123).filter(i -> (i<=57||i>=65) && (i<=90||i>=97)).limit(16).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}


	
}
