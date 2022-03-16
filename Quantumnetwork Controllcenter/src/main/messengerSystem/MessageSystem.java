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
import qnccLogger.LogSensitivity;

import javax.swing.*;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things, like signals and prefixes.
 * Send and receiving messages via these methods, the connectionID determines which connectionEndpoint to interact with.
 *
 * @author Jonas Huehne, Sarah Schumann, Sasha Petri
 *
 */
public class MessageSystem {

	private static Log log = new Log(MessageSystem.class.getName(), LogSensitivity.WARNING);
	
	/** The cipher the message system uses to encrypt / decrypt messages & files */
	private static SymmetricCipher cipher;
	/** The authenticator the message system uses to sign / verify messages & files */
	private static SignatureAuthentication authenticator;
	
	private static final String ENCODING_STANDARD = Configuration.getProperty("Encoding");
	
	/** Contains the ConnectionEndpoints for which the MessageSystem handles the high-level messaging. <br>
	 * 	Generally, this is set once when initializing the program, however, for automated tests it may be needed to set this multiple times to simulate different users. */
	public static ConnectionManager conMan;
	private static final byte[] DEBUG_KEY = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};

	
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
	public static void setAuthenticationAlgorithm(SignatureAuthentication authentication) {
		MessageSystem.authenticator = authentication;
	}


	/**
	 * @return the message authenticator currently in use by the MessageSystem
	 */
	public static SignatureAuthentication getAuthenticator() {
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
				informPartnerAboutKeyUse(); // inform partner for key synchronization purposes
				// encrypt the file locally
				byte[] byteKey = SimpleKeyStore.getNextKeyBytes(connectionID, cipher.getKeyLength());
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
			sendMessage(connectionID, TransmissionTypeEnum.FILE_TRANSFER, args, fileBytes, (sign || encryptFile), confirm);
			
		} catch (EndpointIsNotConnectedException | ManagerHasNoSuchEndpointException | IOException | InvalidKeyException | 
				IllegalBlockSizeException | SQLException | NotEnoughKeyLeftException | NoKeyForContactException e) {
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
			// inform partner that you wish to send an encrypted message (to prevent key desync)
			informPartnerAboutKeyUse();
			// encrypt the message
			String keyIDofConnection = conMan.getConnectionEndpoint(connectionID).getKeyStoreID();
			byte[] msgBytes = stringToByteArray(msgString);
			byte[] key = SimpleKeyStore.getNextKeyBytes(keyIDofConnection, cipher.getKeyLength() / 8);
			byte[] encMsgBytes = cipher.encrypt(msgBytes, key);
			// Provide the index in the message args so receiver knows where to start with decryption
			int index = SimpleKeyStore.getIndex(keyIDofConnection);
			MessageArgs args = new MessageArgs(index);
			sendMessage(connectionID, TransmissionTypeEnum.TEXT_MESSAGE, args, encMsgBytes, true, confirm);
		} catch (EndpointIsNotConnectedException | ManagerHasNoSuchEndpointException | 
			SQLException | NotEnoughKeyLeftException | NoKeyForContactException | InvalidKeyException | IllegalBlockSizeException e) {
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
	
	private static void informPartnerAboutKeyUse() {
		// TODO
	}
	
	/**Utility for converting a byte[] to a String.
	 * The Network sends messagesPackages with byte[]s as content. This Method is used 
	 * to convert a byte[] to a String.
	 * @param arr the byte[] that should be converted to a String.
	 * @return	the resulting String, may be null if the encoding failed.
	 */
	public static String byteArrayToString(byte[] arr) {
		if(arr == null) {
			return null;
		}
		try {
			return new String(arr, Configuration.getProperty("Encoding"));
		} catch (UnsupportedEncodingException e) {
			log.logWarning("Error: unsupported Encoding: " + Configuration.getProperty("Encoding") + "!", e);
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
		if(str == null || str.equals("")) {
			return null;
		}
		try {
			return str.getBytes(Configuration.getProperty("Encoding"));
		} catch (UnsupportedEncodingException e) {
			log.logWarning("Error: unsupported Encoding: " + Configuration.getProperty("Encoding") + "!", e);
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
