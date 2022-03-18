package externalAPI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import encryptionDecryption.AES256;
import encryptionDecryption.FileCrypter;
import encryptionDecryption.SymmetricCipher;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.CouldNotDecryptMessageException;
import exceptions.CouldNotEncryptMessageException;
import exceptions.CouldNotGetKeyException;
import exceptions.CouldNotSendMessageException;
import exceptions.ExternalApiNotInitializedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import exceptions.PortIsInUseException;
import frame.Configuration;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import messengerSystem.SignatureAuthentication;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;

/**
 * An API class for external use to get keys and use the encryption/decryption with or without sending the file.
 * @implNote The API should be functional, but is still very underdeveloped. 
 * @author Lukas Dentler, Sasha Petri
 */
public class ExternalAPI {
	
	private static boolean initialized = false;
	private static ConnectionManager conMan; // connection manager, used when joining the network
	private static SymmetricCipher cipher; // cipher used for encryption / decryption
	

	/**
	 * Gets a number of key bytes for the specified communicationPartner from the keyStore.
	 * @param communicationPartner
	 * 		the partner to get key bytes for <br>
	 * 		must have an entry in the key store with an id that is equal to this name
	 * @param nbytes
	 * 		the amount of key bytes to get
	 * @param index
	 * 		the index at which to start getting key bytes from
	 * @return
	 * 		{@code key[index]} to {@code key[index + n]}, where {@code key} is the key saved with id {@code communicationPartner}
	 * @throws CouldNotGetKeyException 
	 * 		if the specified key could not be retrieved
	 * @see KeyStoreDbManager#getKeyBytesAtIndexN(String, int, int)
	 */
	public static byte[] exportKeyByteArray(String communicationPartner, int nbytes, int index) throws CouldNotGetKeyException {
		try {
			return KeyStoreDbManager.getKeyBytesAtIndexN(communicationPartner, nbytes, index);
		} catch (NotEnoughKeyLeftException | NoKeyWithThatIDException | SQLException e) {
			throw new CouldNotGetKeyException("Could not get the specified key for communication partner " + communicationPartner , e);
		}
	}
	
	/**
	 * Gets a number of key bytes for the specified communicationPartner from the keyStore
	 * and then gives a string consisting of these bytes.
	 * @param communicationPartner
	 * 		the partner to get key bytes for <br>
	 * 		must have an entry in the key store with an id that is equal to this name
	 * @param nbytes
	 * 		the amount of key bytes to get
	 * @param index
	 * 		the index at which to start getting key bytes from
	 * @return
	 * 		{@code key[index]} to {@code key[index + n]}, where {@code key} is the key saved with id {@code communicationPartner}
	 * @throws CouldNotGetKeyException 
	 * 		if the specified key could not be retrieved
	 * @see KeyStoreDbManager#getKeyBytesAtIndexN(String, int, int)
	 */ // <Sasha> not sure where this is useful
	public static String exportKeyString(String communicationPartner, int nbytes, int index) throws CouldNotGetKeyException {
		byte[] keyBytes = exportKeyByteArray(communicationPartner, nbytes, index);
		StringBuilder keyString = new StringBuilder();
		
		for(int i = 0; i < keyBytes.length; i++) {
			keyString.append(keyBytes[i]);
		}
		
		return keyString.toString();
	}
	
	/**
	 * Gets a key ready to be used by the {@link #cipher} used in this API. <br>
	 * Key will be gotten from the keystore entry for the specified 
	 * communication partner at the given index.
	 * 
	 * @param communicationPartner
	 * 		the partner to get key bytes for <br>
	 * 		must have an entry in the key store with an id that is equal to this name
	 * @return a key as a SecretKey Object <br>
	 * key will be {@code key[index]} to {@code key[index + n]}, 
	 * where {@code key} is the key saved with id {@code communicationPartner}
	 * and {@code n} is the key length of the currently used cipher in bytes
	 * @throws CouldNotGetKeyException 
	 * 		if the specified key could not be retrieved
	 */
	public static SecretKey exportKeySecretKey(String communicationPartner, int index) throws CouldNotGetKeyException {
		return cipher.byteArrayToSecretKey(exportKeyByteArray(communicationPartner, cipher.getKeyLength() / 8, index));
	}
	
	/**
	 * helper method to identify the current path to the externalAPI directory
	 * 
	 * @return Path of the externalAPI directory, returns null if externalAPI directory does not exist
	 */
	private static Path getExternalAPIPath() {
		Path currentWorkingDir = Path.of(Configuration.getBaseDirPath());
		Path externalPath = currentWorkingDir.resolve("externalAPI");
		if(!Files.isDirectory(externalPath)) {
			System.err.println("Error, could not find the externalAPI folder, expected: " + externalPath.normalize());
			return null;
		}
		return externalPath;
	}
	
	/**
	 * Encrypts a file using the next key bytes saved for a specified contact.
	 * File will be saved in the external API directory. The file name of the encrypted
	 * file will include the index at which the key starts that was used for encryption.
	 * @param communicationPartner
	 * 		whose key the file is to be encrypted with <br>
	 * 		this method will retrieve a key from the keystore saved under this ID
	 * @param inFile
	 * 		the file to encrypt 
	 * @throws CouldNotEncryptMessageException 
	 * 		if the file could not be encrypted, wraps another Exception
	 */
	public static void encryptFile(String communicationPartner, File inFile) throws CouldNotGetKeyException, CouldNotEncryptMessageException {
		try {
			int index = KeyStoreDbManager.getIndex(communicationPartner);
			SecretKey sk = exportKeySecretKey(communicationPartner, index);
			Path outPath = Paths.get(getExternalAPIPath().toString(), "encr_index_" + index + "_" + inFile.getName());
			
			FileCrypter.encryptAndSave(inFile, cipher, sk, outPath);
		} catch (CouldNotGetKeyException | InvalidKeyException | IllegalBlockSizeException | IOException | NoKeyWithThatIDException | SQLException e) {
			throw new CouldNotEncryptMessageException("Could not encrypt the file " + inFile + " with key of " + communicationPartner, e);
		}
	}
	
	/**
	 * Decrypts a file using the key saved for the specified ID.
	 * Uses the bytes starting at the given index in the keystore entry.
	 * @param communicationPartner
	 * 		who sent the files <br>
	 * 		this method will retrieve a key from the keystore saved under this ID
	 * @param index
	 * 		the bytes to be used for decryption will be read starting at that
	 * 		index in the keystore (needs to be the same as the index was when
	 * 		the file was decrypted with the same key)
	 * @param inFile
	 * 		the file to decrypt
	 * @throws CouldNotDecryptMessageException 
	 * 		if the file could not be decrypted
	 * @see KeyStoreDbManager
	 */ // Generally, it will not be necessary to call this method (decryption of files is automatic in the message system)
	public static void decryptFile(String communicationPartner, int index, File inFile) throws CouldNotDecryptMessageException {
		try {
			byte[] key = KeyStoreDbManager.getKeyBytesAtIndexN(communicationPartner, cipher.getKeyLength() / 8, index);
			SecretKey sk = cipher.byteArrayToSecretKey(key);
			Path outPath = Paths.get(getExternalAPIPath().toString(), "decr_" + inFile.getName());
			
			FileCrypter.decryptAndSave(inFile, cipher, sk, outPath);
		} catch ( NotEnoughKeyLeftException | NoKeyWithThatIDException | SQLException | InvalidKeyException | BadPaddingException | IOException e) {
			throw new CouldNotDecryptMessageException("Could not decrypt the file " + inFile + " with key of " + communicationPartner + " starting at index " + index, e);
		}
	}
	
	/**
	 * Encrypts the contents of a .txt file and sends them as a text message to the specified partner.
	 * Requires externalAPI to be initialized.
	 * @param communicationPartner 
	 * 	ID of the partner to send the message to <br>
	 *	because we are sending an encrypted message, this partner needs a key in the keystore (saved under their ID)
	 * @param file
	 * 	the file to send, must be a .txt file
	 * @throws CouldNotSendMessageException
	 * 	if the message could not be sent, e.g. because there is no key available, or the file could not be read
	 * @throws ExternalApiNotInitializedException 
	 * 	if {@link #initialize(String, String, int)} has not been executed yet
	 */
	public static void sendEncryptedTxtFile(String communicationPartner, File file) throws CouldNotSendMessageException, ExternalApiNotInitializedException {
		if (!file.toString().endsWith(".txt")) {
			throw new IllegalArgumentException("File " + file.toString() + " is not a .txt file.");
		}
		if (!initialized) throw new ExternalApiNotInitializedException("Can not send message to " + communicationPartner + ". API is not initialized.");
		try {
			// load the text of the file
			String msg = Files.readString(file.toPath());
			// send it as an encrypted message
			MessageSystem.sendEncryptedTextMessage(communicationPartner, msg, false);
		} catch (IOException e) {
			throw new CouldNotSendMessageException("Could not send the encrypted txt file - an I/O Exception occured.", e);
		}
	}
	
	/**
	 * @return all messages received on this node in the network <br>
	 * each entry in the list is of the form (i, msg) where i is the ID of the CE on which the message was received
	 * and msg is the message itself
	 */
	public static ArrayList<SimpleEntry<String, String>> getAllReceivedMessages() {
		if (!initialized) return null;
		
		ArrayList<SimpleEntry<String, String>> receivedMessages = new ArrayList<SimpleEntry<String, String>>();
		for (Entry<String, ConnectionEndpoint> e : conMan.returnAllConnections().entrySet()) {
			for (Entry<String, String> chatMessage : e.getValue().getChatLog()) { // for each CE in the CM, get the chatlog
				if (chatMessage.getKey().contains(e.getValue().getRemoteName())) { // if the message came from the other party
					receivedMessages.add(new SimpleEntry<>(e.getValue().getID(), chatMessage.getValue())); // add it to the list of received messages
				}
			}
		}
		
		return receivedMessages;
	}
	
	/**
	 * Writes all received messages to a txt file.
	 * @throws IOException 
	 * 		if there was an issue with writing the files
	 */
	public static void writeReceivedMessagesToTXT() throws IOException { // replaces receiveEncryptedTxtFile of the old implementation
		ArrayList<SimpleEntry<String, String>> messages = getAllReceivedMessages();
		if (messages == null) return;
		
		Date date = Calendar.getInstance().getTime();  
		String dateString = (new SimpleDateFormat("yyyy-mm-dd hh-mm-ss")).format(date);
		Path outpath = Paths.get(Configuration.getBaseDirPath(), "externalAPI", "receivedTexts", dateString + ".txt");
		try (BufferedWriter buffer = new BufferedWriter(new FileWriter(outpath.toFile()));) {
			for (SimpleEntry<String, String> message : messages) {
				buffer.write("Received Message [" + message.getValue() + "] on CE [" + message.getKey() + "]" );
			}
		}
	}
	
	/**
	 * Joins the Peer-to-Peer Network as a member with the given name, IP and port.
	 * Needed to send and receive messages.
	 * @param localName
	 * 		the name to join the network with
	 * @param localAddress
	 * 		the local address to join the network with
	 * @param localPort
	 * 		local server port that other members will connect to
	 * @throws IOException
	 * 		may occur if there was an I/O error while initializing the ConnectionManager
	 * @throws PortIsInUseException
	 * 		if a ConnectionManager is running on this machine already using that port
	 */
	public static void initialize(String localName, String localAddress, int localPort) throws IOException, PortIsInUseException {
		if (initialized) {// do not initialize twice
			return; 
		}
		
		CommunicationList commList = new SQLiteCommunicationList();
		conMan = new ConnectionManager(localAddress, localPort, localName, commList);
		
		SignatureAuthentication auth = new SHA256withRSAAuthentication();
		// if there is no (sk, pk) pair create it
		auth.generateSignatureKeyPair("signature", true, false, false);
		cipher = new AES256();
		MessageSystem.setAuthenticationAlgorithm(auth);
		MessageSystem.setEncryption(cipher);
		
		initialized = true;
	}
	
	
	/**
	 * Connects to another endpoint in the network.
	 * @param ip
	 * 		ip of the endpoint to connect to
	 * @param port
	 * 		port of the endpoint to connect to
	 * @param name
	 * 		name (ID) under which to save the endpoint, later used for sending messages 
	 * @param pk
	 * 		pk of the endpoint, required for validating messages from that endpoint
	 * @throws IpAndPortAlreadyInUseException 
	 * 		if a connection with this IP:Port pair already exists 
	 * @throws ConnectionAlreadyExistsException 
	 * 		if a connection with this ID already exists
	 * @throws ExternalApiNotInitializedException
	 * 		if {@link #initialize(String, String, int)} has not been executed yet 
	 */
	public static void connectTo(String ip, int port, String name, String pk) throws ConnectionAlreadyExistsException, IpAndPortAlreadyInUseException, ExternalApiNotInitializedException {
		if (!initialized) 
			throw new ExternalApiNotInitializedException(
					"Can not connect to CE " + name + " with address:port " 
					+ ip + ":" + port + ". API is not initialized.");
		conMan.createNewConnectionEndpoint(name, ip, port, pk);
	}
	
	/**
	 * @return true if the ExternalAPI has been initialized, i.e. is ready for data transfer
	 */
	public static boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * @return the cipher currently in use by the external API, null if not initialized
	 */
	public static SymmetricCipher getCipher() {
		return cipher;
	}

}
