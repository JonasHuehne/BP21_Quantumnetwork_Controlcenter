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
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import encryptionDecryption.FileCrypter;
import encryptionDecryption.SymmetricCipher;
import exceptions.CouldNotSendMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;
import keyStore.KeyStoreDbManager;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.MessageArgs;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

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
		
	/** Contains the ConnectionEndpoints for which the MessageSystem handles the high-level messaging. <br>
	 * 	Generally, this is set once when initializing the program, however, for automated tests it may be needed to set this multiple times to simulate different users. */
	public static ConnectionManager conMan;
	
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
	public static void sendMessage(String connectionID, TransmissionTypeEnum type, MessageArgs args, byte[] content, boolean sign, boolean confirm)
			throws EndpointIsNotConnectedException, ManagerHasNoSuchEndpointException {
		NetworkPackage message = new NetworkPackage(type, args, content, confirm);
		if (sign) message.sign(authenticator);
		conMan.sendMessage(connectionID, message);
	}
	
	/**
	 * Internal method for sending a file.
	 * @param connectionID
	 * 		connectionID of a {@linkplain ConnectionEndpoint} in the {@linkplain ConnectionManager} of this class <br>
	 * 		the constructed NetworkPackage is sent to the connected partner of the specified endpoint
	 * @param file
	 * 		the file to send
	 * @param encryptFile
	 * 		true if the file is to be encrypted
	 * @param sign
	 * 		true if the file is to be signed <br>
	 * 		always treated as true if the file is to be encrypted
	 * @param confirm
	 * 		true if a message of type {@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION}
	 * 		should be sent in response to this message
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
				byte[] byteKey = KeyStoreDbManager.getNextNBytes(connectionID, cipher.getKeyLength() / 8, false);
				keyIndex = KeyStoreDbManager.getIndex(connectionID);
				SecretKey key = cipher.byteArrayToSecretKey(byteKey);
				Path pathToEncryptedFile = Paths.get(file.getParent().toString(), "encrypted_" + file.getName());
				FileCrypter.encryptAndSave(file, cipher, key, pathToEncryptedFile);
				// read the encrypted file to send
				fileBytes = Files.readAllBytes(pathToEncryptedFile);
				// delete encrypted file after reading it to not clutter the machine
				Files.deleteIfExists(pathToEncryptedFile);
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
				NoKeyWithThatIDException | SecurityException | ManagerHasNoSuchEndpointException e) {
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
		log.logInfo("Attempting to send message <" + msgString + "> from CE with ID <" + connectionID + "> | Signed: " + sign + " Confirmed: " + confirm + " |");
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
	 * 		the message to send
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
			byte[] key = KeyStoreDbManager.getNextNBytes(keyIDofConnection, cipher.getKeyLength() / 8, false);
			byte[] encMsgBytes = cipher.encrypt(msgBytes, key);
			
			// Provide the index in the message args so receiver knows where to start with decryption
			int index = KeyStoreDbManager.getIndex(keyIDofConnection);
			
			log.logInfo("Attempting to send encrypted <" + msgString + "> from CE with ID <" + connectionID + "> | "
					+ "Started Encryption at Index: " + index + " Confirmed: " + confirm + " |");
				
			// Construct the message to send
			MessageArgs args = new MessageArgs(index);
			NetworkPackage msg = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, args, encMsgBytes, confirm);
			msg.sign(authenticator);
			// Tell the other party we wish to send, and queue the message
			informAndSendOnceConfirmed(connectionID, msg);
		} catch (EndpointIsNotConnectedException | SQLException | NotEnoughKeyLeftException 
				| NoKeyWithThatIDException | InvalidKeyException | IllegalBlockSizeException 
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
	
	/**
	 * Called after encrypting a message. This queues the encrypted message up for sending,
	 * however, before sending it alerts the other CE that key bytes starting at a certain index
	 * are used in the encrypted message it wants to send. The other CE can then approve or disapprove
	 * this (disapprove if this would cause sender to use bytes for encryption that receiver already used)
	 * and only if the receiver approves, the message is sent. <br>
	 * If this takes longer than three seconds, no message is sent. <br>
	 * The timeout happens asynchronously - this method does not block.
	 * @param connectionID
	 * 		ID of the CE in the {@linkplain ConnectionManager} from which to send the package
	 * @param msg
	 * 		the encrypted message to send
	 * @throws NoKeyWithThatIDException
	 * 		if there is no mutual key for the connection given by the specified CE <br>
	 * 		if this method is called correctly, this should not occur
	 * @throws SQLException
	 * 		if an SQL error occured with the keystore
	 * @throws EndpointIsNotConnectedException
	 * 		if the specified endpoint is not connected to their partner
	 */
	private static void informAndSendOnceConfirmed(String connectionID, NetworkPackage msg) 
			throws NoKeyWithThatIDException, SQLException, EndpointIsNotConnectedException {
		
		/*
		 * Basic implementation of algorithm to avoid key desynch.
		 * Might need some improvements in the future.
		 */

		// CE A will send this package to CE B to inform them that
		// A wishes to use bytes of their mutual key, starting at the specified index
		final ConnectionEndpoint ceA = conMan.getConnectionEndpoint(connectionID);
		int currentIndex = KeyStoreDbManager.getIndex(ceA.getKeyStoreID());
		final MessageArgs args = new MessageArgs(currentIndex);
		final NetworkPackage keyUseAlert = new NetworkPackage(TransmissionTypeEnum.KEY_USE_ALERT, args, false);
		ceA.pushOnceConfirmationReceivedForID(keyUseAlert.getID(), msg); // push the main message once key use is confirmed
		ceA.pushMessage(keyUseAlert);
		
		// After sending this, increment the index because we used those bits now
		KeyStoreDbManager.incrementIndex(connectionID, getCipher().getKeyLength() / 8);
		
		/*
		 * Asynchronously wait 3 seconds, if no confirmation arrives, delete the package from the queue.
		 * (This is to avoid cluttering our RAM infinitely with packages)
		 */
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				NetworkPackage removed = conMan.getConnectionEndpoint(connectionID).removeFromPushQueue(keyUseAlert.getID());
				if (removed != null) { 
					// if we successfully removed the package, that means it wasn't removed through a KEY_USE_ACCEPT / KEY_USE_REJECT
					log.logWarning("A timeout occurred while awaiting a confirmation for key use on connection with ID " + connectionID);
				}
			}
		};
		Timer timer = new Timer("KeySynchro TimeOut Timer");
		timer.schedule(task, 3000L);
		
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
}
