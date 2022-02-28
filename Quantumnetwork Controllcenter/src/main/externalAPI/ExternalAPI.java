package externalAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.crypto.SecretKey;

import encryptionDecryption.AES256;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;

/**
 * An API class for external use to get keys and use the encryption/decryption with or without sending the file.
 * 
 * @author Lukas Dentler
 */
public class ExternalAPI {
	
	/**
	 * returning a key for the stated communication partner
	 * 
	 * if "42debugging42" is used as parameter for communicationPartner a fixed key is returned for debugging and testing purposes
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
	}
	
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
	 */
	public static void encryptFile(String communicationPartner, String fileName) {
		Path externalPath = getExternalAPIPath();
		Path toEncrypt = externalPath.resolve(fileName);
		
		byte[] key = exportKeyByteArray(communicationPartner);
		
		if(Files.exists(toEncrypt)) {
			File encrypt = toEncrypt.toFile();
			File encrypted = new File(externalPath.resolve("encrypted_" + fileName).toString());
			AES256.encryptFile(encrypt, key, encrypted);
		}
		else {
			System.err.println("Error, could not find the file to encrypt, expected" + toEncrypt.normalize());
		}
	}
	
	/**
	 * method to decrypt a given file in the externalAPI directory
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName name of the file to be decrypted
	 */
	public static void decryptFile(String communicationPartner, String fileName) {
		Path externalPath = getExternalAPIPath();
		Path toDecrypt = externalPath.resolve(fileName);
		
		byte[] key = exportKeyByteArray(communicationPartner);
		
		if(Files.exists(toDecrypt)) {
			File decrypt = toDecrypt.toFile();
			File decrypted = new File(externalPath.resolve("decrypted_" + fileName).toString());
			AES256.decryptFile(decrypt, key, decrypted);
		}
		else {
			System.err.println("Error, could not find the file to decrypt, expected" + toDecrypt.normalize());
		}
		
		/*
		 * TODO: maybe rework this function to use with a given key index or something similar to reduce unwanted effects
		 * when using this message and communicating over the messengerSystem in close temporal proximity.
		 */
	}
	
	/**
	 * sends a signed message with encrypted text from given .txt file
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName Name of the .txt file containing the suffix ".txt"
	 */
	public static boolean sendEncryptedTxtFile(String communicationPartner, String fileName) {
		Path externalPath = getExternalAPIPath();
		Path toRead = externalPath.resolve(fileName);
		
		String message = "";
		
		try {
		message = Files.readString(toRead);
		} catch (IOException e) {
			
			System.err.println("Error, could not read the given File \n" + e.toString());
			return false;
		}
		
		return MessageSystem.sendEncryptedMessage(communicationPartner, message);
	}
	
	/**
	 * receives a signed message with encrypted text, sent from an external source.
	 * Saves the text decrypted in a .txt file in externalAPI directory, named with the ID of the sender and current Timestamp.
	 * 
	 * @param communicationPartner name of the sender as saved in communicationList {@link communicationList.Contact#getName()}
	 */
	public static void receiveEncryptedTxtFile(String communicationPartner) {
		Path externalPath = getExternalAPIPath();
		
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu_MM_dd_HH_mm");
        LocalDateTime now = LocalDateTime.now();
        String currentDateTime = dateTimeFormatter.format(now);
        
		Path toWrite = externalPath.resolve(currentDateTime + "_" + communicationPartner + ".txt");
		
		File decrypted = new File(toWrite.toString());
		
		String received = MessageSystem.readEncryptedMessage(communicationPartner);
		
		try {
			Files.writeString(decrypted.toPath(), received);
		} catch (IOException e) {
			System.err.println("Error, could not write to the outputfile \n" + e.toString());
		}
	}
	
	
	/**
	 * encrypts and sends a given file from externalAPI directory 
	 * 
	 * @param communicationPartner name of the receiver as saved in communicationList {@link communicationList.Contact#getName()}
	 * @param fileName Name of the file to be sent
	 * @return true if the file has been sent successfully, false otherwise
	 */
	public static boolean sendEncryptedFile(String communicationPartner, String fileName) {
		Path externalPath = getExternalAPIPath();
		Path toSend = externalPath.resolve(fileName);
		
		return MessageSystem.sendEncryptedFile(fileName, toSend);
	}
	
	/**
	 * receives and decrypts a file sent by the specified communicationPartner and saves it in the externalAPI directory
	 * {@link messengerSystem.MessageSystem#receiveEncryptedFileToPath(String, Path)} with externalAPI directory as Path
	 * 
	 * @param communicationPartner name of the sender as saved in communicationList {@link communicationList.Contact#getName()}
	 */
	public static void receiveEncryptedFile(String communicationPartner) {
		Path externalPath = getExternalAPIPath();
		
		MessageSystem.receiveEncryptedFileToPath(communicationPartner, externalPath);
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
