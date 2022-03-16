package messengerSystem;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import encryptionDecryption.AES256;
import encryptionDecryption.SymmetricCipher;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;

import java.util.Random;

import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import keyStore.SimpleKeyStore;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
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

	 * Sends a text message.
	 * @param connectionID
	 * 		the ID of the {@linkplain ConnectionEndpoint} that is to send a message to its partner <br>
	 * 		must be a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class
	 * @param msgString
	 * 		the text message to send
	 * @param sign
	 * 		whether the message should be signed
	 * @param confirm
	 * 		whether the recipient should send back a confirmation message ({@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION} if they receive the message
	 * @param keyIndex
	 * 		if this is not an encrypted message, set this to -1 <br>
	 * 		if this is an encrypted message this is the index at which the used section of the key
	 * 		between the sender of the message and the recipient starts,
	 * 		allowing the recipient to know which bits are to be used for decryption
	 * @throws EndpointIsNotConnectedException
	 * 		if the specified connection endpoint is not connected to their partner
	 * @throws ManagerHasNoSuchEndpointException
	 */
	public static void sendTextMessage(String connectionID, String msgString, boolean sign, boolean confirm, int keyIndex) throws EndpointIsNotConnectedException, ManagerHasNoSuchEndpointException {
		MessageArgs args = new MessageArgs(keyIndex);
		TransmissionTypeEnum type = TransmissionTypeEnum.TEXT_MESSAGE;
		byte[] messageBytes = stringToByteArray(msgString);
		NetworkPackage message = new NetworkPackage(type, args, messageBytes, confirm);
		if (sign) message.sign(authenticator);
		conMan.sendMessage(connectionID, message);
	}
	
	/**
	 * Sends a file.
	 * @param connectionID
	 * 		the ID of the {@linkplain ConnectionEndpoint} that is to send a message to its partner <br>
	 * 		must be a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class
	 * @param file
	 * 		the file to send
	 * @param sign
	 * 		whether the message should be signed
	 * @param confirm
	 * 		whether the recipient should send back a confirmation message ({@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION} if they receive the message 
	 * @param keyIndex
	 * 		if this is not an encrypted message, set this to -1 <br>
	 * 		if this is an encrypted message this is the index at which the used section of the key 
	 * 		between the sender of the message and the recipient starts,
	 * 		allowing the recipient to know which bits are to be used for decryption 
	 * @throws EndpointIsNotConnectedException
	 * 		if the specified connection endpoint is not connected to their partner
	 * @throws ManagerHasNoSuchEndpointException 
	 */
	public static void sendFile(String connectionID, File file, boolean sign, boolean confirm, int keyIndex) throws EndpointIsNotConnectedException, ManagerHasNoSuchEndpointException {
		String fileName = file.getName();
		TransmissionTypeEnum type = TransmissionTypeEnum.FILE_TRANSFER;
		MessageArgs args = new MessageArgs(fileName, keyIndex);
		byte[] messageBytes = null; // TODO load file here
		NetworkPackage message = new NetworkPackage(type, args, messageBytes, confirm);
		if (sign) message.sign(authenticator);
		conMan.sendMessage(connectionID, message);
	}

	
	
	private static void sendEncryptedFile() {
		// Encrpyt file locally
		
		// Send the file as a regular message, with a flag that the contents are encrypted
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
