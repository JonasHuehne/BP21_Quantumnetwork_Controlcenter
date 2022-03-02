package externalAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import javax.crypto.SecretKey;

import encryptionDecryption.AES256;
import encryptionDecryption.CryptoUtility;
import exceptions.NoKeyForContactException;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;

/**
 * An API class for external use to get keys and use the encryption/decryption with or without sending the file.
 * 
 * @author Lukas Dentler, Sasha Petri
 */
public class ExternalAPI {
	
	/**
	 * returning a key for the stated communication partner
	 * 
	 * if "42debugging42" is used as parameter for communicationPartner a fixed key is returned for debugging and testing purposes
	 * 
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList 
	 * @return a key as byte array
	 */
	public static byte[] exportKeyByteArray(String communicationPartner) {
		//key for debugging purposes
		if(communicationPartner.equals("42debugging42")) {
			return new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32}; 
		}
		return KeyStoreDbManager.getEntryFromKeyStore(communicationPartner).getKeyBuffer();
	} // TODO: Incrementation of the used key bits, potentially just use MessageSystem.getKeyOrDefault() here
	
	/**
	 * returning a key for the stated communication partner
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList 
	 * @return a key as String
	 */
	public static String exportKeyString(String communicationPartner) {
		byte[] keyBytes = exportKeyByteArray(communicationPartner);
		StringBuilder keyString = new StringBuilder();
		
		for(int i = 0; i < keyBytes.length; i++) {
			keyString.append(keyBytes[i]);
		}
		
		return keyString.toString();
	}
	
	/**
	 * returning a key for the stated communication partner
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList 
	 * @return a key as SecretKey object
	 */
	public static SecretKey exportKeySecretKey(String communicationPartner) {
		return encryptionDecryption.CryptoUtility.byteArrayToSecretKeyAES256(exportKeyByteArray(communicationPartner));
	}
	
	/**
	 * helper method to identify the current path to the externalAPI directory
	 * 
	 * @return Path of the externalAPI directory, returns null if externalAPI directory does not exist
	 */
	private static Path getExternalAPIPath() {
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
		Path externalPath = currentWorkingDir.resolve("externalAPI");
		if(!Files.isDirectory(externalPath)) {
			System.err.println("Error, could not find the externalAPI folder, expected: " + externalPath.normalize());
			return null;
		}
		return externalPath;
	}
	
	/**
	 * method to encrypt a given file in the externalAPI directory
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName name of the file to be encrypted
	 * @throws IOException 
	 * 		if an I/O error occured trying to read or write the file
	 */
	public static void encryptFile(String communicationPartner, String fileName) throws IOException {
		Path externalPath = getExternalAPIPath();
		Path toEncrypt = externalPath.resolve(fileName);
		
		byte[] key = exportKeyByteArray(communicationPartner);
		byte[] encryptedBits = CryptoUtility.loadAndEncryptFile_AES256(toEncrypt, key);
		
		Files.write(externalPath.resolve("encrypted_" + fileName), encryptedBits);
	}
	
	/**
	 * method to decrypt a given file in the externalAPI directory
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName name of the file to be decrypted
	 * @throws IOException 
	 * 		if an I/O Exception occured trying to read the file or save it again
	 */
	public static void decryptFile(String communicationPartner, String fileName) throws IOException {
		Path externalPath = getExternalAPIPath();
		Path toDecrypt = externalPath.resolve(fileName);
		
		byte[] key = exportKeyByteArray(communicationPartner);
		byte[] fileBytes = Files.readAllBytes(toDecrypt);
		CryptoUtility.decryptAndSaveFile_AES256(fileBytes, key, externalPath, "decrypted_" + fileName);
	}
	
	/**
	 * encrypts and sends a given file from externalAPI directory 
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName Name of the file to be sent
	 * @return true if the file has been sent successfully, false otherwise
	 * @throws NoKeyForContactException 
	 * 		if no key could be found for the associated contact
	 * @throws IOException 
	 * 		if an I/O error occured trying to read the file
	 */
	public static boolean sendEncryptedFile(String communicationPartner, String fileName) throws IOException, NoKeyForContactException {
		Path externalPath = getExternalAPIPath();
		Path toSend = externalPath.resolve(fileName);
		
		return MessageSystem.sendEncryptedFile(communicationPartner, toSend);
	}
	
	/**
	 * receives and decrypts a file sent by the specified communicationPartner and saves it in the externalAPI directory
	 * {@link messengerSystem.MessageSystem#receiveEncryptedFileToPath(String, Path)} with externalAPI directory as Path
	 * 
	 * @param communicationPartner name of the sender as saved in communicationList {@link communicationList.Contact#getName()}
	 * @throws IOException 
	 * @throws NoKeyForContactException 
	 */
	public static void receiveEncryptedFile(String communicationPartner) throws NoKeyForContactException, IOException {
		Path externalPath = getExternalAPIPath();
		int i = (new Random()).nextInt();
		MessageSystem.receiveAndWriteEncryptedFile(communicationPartner, externalPath, "received_file" + i);
	}
	
	/*
	 * uncomment for manual testing
	 *
	public static void main(String[] args) {
		encryptFile("42debugging42", "jpgTest.jpg");
		decryptFile("42debugging42", "encrypted_jpgTest.jpg");
		
		encryptFile("42debugging42", "odsTest.ods");
		decryptFile("42debugging42", "encrypted_odsTest.ods");
		
		encryptFile("42debugging42", "odtTest.odt");
		decryptFile("42debugging42", "encrypted_odtTest.odt");
		
		encryptFile("42debugging42", "pdfTest.pdf");
		decryptFile("42debugging42", "encrypted_pdfTest.pdf");
		
		encryptFile("42debugging42", "txtTest.txt");
		decryptFile("42debugging42", "encrypted_txtTest.txt");
		
		encryptFile("42debugging42", "zipTest.zip");
		decryptFile("42debugging42", "encrypted_zipTest.zip");
	}
	*/
}
