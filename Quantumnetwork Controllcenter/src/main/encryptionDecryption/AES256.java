package encryptionDecryption;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


/**
 * Class for AES-256 encryption and decryption of Strings using the CBC Mode with a constant IV
 * 
 * @author Lukas Dentler, Sasha Petri
 */
public class AES256 {
	
	/*
	 * TODO: Consider throwing Exceptions instead of just returning null, e.g. when an invalid key is given as a parameter.
	 * This makes the methods more "friendly" to handle by a caller in case an error occurs, because it allows different cases to be handled individually.
	 * (Example Scenario: Attempt encryption with a user selected key, encryption fails and caller sees it was an InvalidKeyException.
	 *  Caller can then tell the user what went wrong, instead of just "something went wrong".)
	 */
	
	//generating constant IV
	private static final byte[] BYTE_IV = {42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42};
	private static final IvParameterSpec IV = new IvParameterSpec(BYTE_IV);
	
	//constants
	private static final String ALGORITHM_WITH_PADDING = "AES/CBC/PKCS5Padding";
	
	/**
	 * Encrypts the given plain text bytes using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param plaintext the plain text to be encrypted, may not be null
	 * @param key SecretKey object for AES256, may not be null
	 * @return the result of encrypting the plaintext with the given key, or null if an error occured <br>
	 * 		   information on the error will be printed to the standard error output
	 */
	public static byte[] encrypt(byte[] plaintext, SecretKey key) {
		if (plaintext == null) throw new NullPointerException("Plaintext may not be null.");
		if (key == null) throw new NullPointerException("Key may not be null.");
		
		try {
			//get Cipher Instance
			Cipher cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING);
			
			//initialize Cipher for encryption
			cipher.init(Cipher.ENCRYPT_MODE, key, IV);
			
			//perform encryption
			byte[] ciphertext = cipher.doFinal(plaintext);
			return ciphertext;
		}
		//printing exceptions
		catch (InvalidKeyException e) {
			System.err.println("An invalid Key was used. \n" + e.toString());
		}
		catch (IllegalBlockSizeException e) {
			System.err.println("The plaintext has the wrong length. \n" + e.toString());
		}
		catch (Exception e) {
			//TODO later printing exception to UI
			System.err.println("An ERROR occured during encryption:\n" + e.toString());
		}
		return null ;
		
	}
	
	/**
	 * Encrypts the given plain text String using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param strPlaintext the plain text that should be encrypted (only UTF-8 chars are guaranteed to work), may not be null
	 * @param strKey the key used to encrypt the cipher text, must be of length 256 and contain only 0s and 1s
	 * @return A String containing the encrypted plain text, may return null in case of an error
	 */
	public static String encrypt(String strPlaintext, String strKey) {
		//generating SecretKey from String
		SecretKey key = CryptoUtility.stringToSecretKeyAES256(strKey);
		//calling encrypt with key as arg
		return encrypt(strPlaintext, key);
	}
	
	/**
	 * Encrypts the given plain text String using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param strPlaintext the plain text that should be encrypted (only UTF-8 chars are guaranteed to work), may not be null
	 * @param byteKey a byte array of size 32, may not be null
	 * @return A String containing the encrypted plain text, may return null in case of an error
	 */
	public static String encrypt(String strPlaintext, byte[] byteKey) {
		//generating SecretKey from byte array
		SecretKey key = CryptoUtility.byteArrayToSecretKeyAES256(byteKey);	
		//calling encrypt with key as arg
		return encrypt(strPlaintext, key);
	}
	
	/**
	 * Encrypts the given plain text String using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param strPlaintext the plain text that should be encrypted (expected to contain only UTF-8 characters), may not be null
	 * @param key SecretKey object for AES256, may not be null
	 * @return A String containing the encrypted plain text, In case of an Error returns null <br>
	 *  	   information on the error will be printed to the standard error output
	 */
	public static String encrypt(String strPlaintext, SecretKey key) {
		if (strPlaintext == null) throw new NullPointerException("Plaintext may not be null.");
		if (key == null) throw new NullPointerException("Key may not be null.");
		byte[] plaintextBytes = strPlaintext.getBytes(StandardCharsets.UTF_8);
		byte[] ciphertextBytes = encrypt(plaintextBytes, key);
		return Base64.getEncoder().encodeToString(ciphertextBytes);
	}
	
	/**
	 * Decrypts the given cipher text bytes using the AES-256 CBC algorithm and a suitable key
	 * @param cipherText the ciphertext to be decrypted, may not be null
	 * @param key SecretKey object for AES256, may not be null
	 * @return the result of decrypting the ciphertext with the given key, or null if an error occured <br>
	 * 		   information on the error will be printed to the standard error output
	 */
	public static byte[] decrypt(byte[] cipherText, SecretKey key) {
		if (key == null) throw new NullPointerException("Key may not be null.");
		if (cipherText == null) throw new NullPointerException("Ciphertext may not be null.");
		if (cipherText.length == 0) throw new IllegalArgumentException("Ciphertext may not be empty.");
		
		
		try {
			//get Cipher Instance
			Cipher cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING);
			
			//initialize Cipher for decryption
			cipher.init(Cipher.DECRYPT_MODE, key, IV);		
			
			//perform decryption
			byte[] bytePlaintext = cipher.doFinal(cipherText);
			
			return bytePlaintext;
		}
		//printing exceptions
		catch (InvalidKeyException e) {
			System.err.println("An invalid Key was used. \n" + e.toString());
		}
		catch (IllegalBlockSizeException e) {
			System.err.println("The ciphertext has the wrong length, make sure to decrypt only text that has been encrypted first \n" + e.toString());
		}
		catch (BadPaddingException e) {
			System.err.println("An ERROR occured regarding the padding of the encrypted text. \n A common reason for this ERROR is that a different key was used for encryption and decryption\n" + e.toString());
		}
		catch (Exception e) {
			//TODO later printing exception to UI
			System.err.println("An ERROR occured during decryption:\n" + e.toString());
		}
		return null;
	}
	
	/**
	 * Decrypts the given cipher text String using the AES-256 CBC algorithm and the corresponding key used to encrypt the cipher text
	 * 
	 * @param strCiphertext the cipher text that should be decrypted as a Base64 encoded String, may not be null
	 * @param strKey the key used to decrypt the cipher text, must be of length 256 and contain only 0s and 1s
	 * @return A String containing the decrypted cipher text, In case of an Error returns null
	 */
	public static String decrypt(String strCiphertext, String strKey) {
		// Generating SecretKey from String
		SecretKey key = CryptoUtility.stringToSecretKeyAES256(strKey);
		return decrypt(strCiphertext, key);
	}
	
	/**
	 * Decrypts the given cipher text String using the AES-256 CBC algorithm and the corresponding key used to encrypt the cipher text
	 * 
	 * @param strCiphertext the cipher text that should be decrypted as a Base64 encoded String, may not be null
	 * @param byteKey the key used to encrypt the cipher text as byte array, may not be null
	 * @return A String containing the decrypted cipher text, In case of an Error returns null
	 */
	public static String decrypt(String strCiphertext, byte[] byteKey) {
		// Generating SecretKey from byte array
		SecretKey key = CryptoUtility.byteArrayToSecretKeyAES256(byteKey);
		return decrypt(strCiphertext, key);
	}
	
	/**
	 * Decrypts the given cipher text String using the AES-256 CBC algorithm and the corresponding key used to encrypt the cipher text
	 * 
	 * @param strCiphertext the cipher text that should be decrypted as a Base64 encoded String, may not be null
	 * @param key SecretKey object for AES256 that was used to encrypt the cipher text, may not be null
	 * @return A String containing the decrypted cipher text, In case of an Error returns null
	 */
	public static String decrypt(String strCiphertext, SecretKey key) {
		if (strCiphertext == null) throw new NullPointerException("Ciphertext may not be null.");
		if (key == null) throw new NullPointerException("Key may not be null.");
		byte[] ciphertextBytes = Base64.getDecoder().decode(strCiphertext);
		byte[] plaintextBytes = decrypt(ciphertextBytes, key);
		return new String(plaintextBytes, StandardCharsets.UTF_8);
	}

}
