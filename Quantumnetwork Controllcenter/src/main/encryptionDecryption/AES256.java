package main.encryptionDecryption;


import java.nio.charset.StandardCharsets;
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
 * @author Lukas Dentler
 */
public class AES256 {
	
	//generating constant IV
	private static final byte[] BYTE_IV = {42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42};
	private static final IvParameterSpec IV = new IvParameterSpec(BYTE_IV);
	
	//constants
	private static final String ALGORITHM_WITH_PADDING = "AES/CBC/PKCS5Padding";
	private static final int KEY_LENGTH_BYTE = 32;
	private static final int KEY_LENGTH_BIT = 256;
	private static final String KEY_WRONG_SIZE = "An invalid key was used. Please use a key of length 256";
	
	/**
	 * Encrypts the given plain text String using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param strPlaintext the plain text that should be encrypted (only UTF-8 chars are guaranteed to work)
	 * @param strKey a bit string with 256 bits
	 * @return A String containing the encrypted plain text, In case of an Error returns null
	 * @throws NumberFormatException if strKey contains any other char besides 0 or 1
	 */
	public static String encrypt(String strPlaintext, String strKey) {
		
		//checking that key has the right length
		if(strKey.length() != KEY_LENGTH_BIT) {
			System.err.println(KEY_WRONG_SIZE);
			return null;
		}
		
		//generating SecretKey from String
		SecretKey key = CryptoUtility.stringToSecretKeyAES256(strKey);
		
		//calling encrypt with key as arg
		return encrypt(strPlaintext, key);
		
	}
	
	/**
	 * Encrypts the given plain text String using the AES-256 CBC algorithm and a suitable key
	 * 
	 * @param strPlaintext the plain text that should be encrypted (only UTF-8 chars are guaranteed to work)
	 * @param key SecretKey object for AES256
	 * @return A String containing the encrypted plain text, In case of an Error returns null
	 */
	public static String encrypt(String strPlaintext, SecretKey key) {
		
		//checking that key has the right length
		if(key.getEncoded().length != KEY_LENGTH_BYTE) {
			System.err.println(KEY_WRONG_SIZE);
			return null;
		}
		
		//converting plain text to byteArray
		byte[] bytePlaintext = strPlaintext.getBytes(StandardCharsets.UTF_8);
		
		try {
			//get Cipher Instance
			Cipher cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING);
			
			//initialize Cipher for encryption
			cipher.init(Cipher.ENCRYPT_MODE, key, IV);
			
			//perform encryption
			byte[] byteCiphertext = cipher.doFinal(bytePlaintext);
			
			//convert cipher text to string
			String strCiphertext = Base64.getEncoder().encodeToString(byteCiphertext);
			
			return strCiphertext;
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
	 * Decrypts the given cipher text String using the AES-256 CBC algorithm and the corresponding key used to encrypt the cipher text
	 * 
	 * @param strCiphertext the cipher text that should be decrypted as a Base64 encoded String
	 * @param strKey the key used to encrypt the cipher text as bit string
	 * @return A String containing the decrypted cipher text, In case of an Error returns null
	 * @throws NumberFormatException if strKey contains any other char besides 0 or 1
	 */
	public static String decrypt(String strCiphertext, String strKey) {
	
		//checking that key has the right length
		if(strKey.length() != KEY_LENGTH_BIT) {
			System.err.println(KEY_WRONG_SIZE);
			return null;
		}
		
		//generating SecretKey from String
		SecretKey key = CryptoUtility.stringToSecretKeyAES256(strKey);
		
		//calling decrypt with key as arg
		return decrypt(strCiphertext, key);
	}
	
	/**
	 * 
	 * @param strCiphertext the cipher text that should be decrypted as a Base64 encoded String
	 * @param key SecretKey object for AES256 that was used to encrypt the cipher text
	 * @return A String containing the decrypted cipher text, In case of an Error returns null
	 */
	public static String decrypt(String strCiphertext, SecretKey key) {
		
		//checking that key has the right length
		if(key.getEncoded().length != KEY_LENGTH_BYTE) {
			System.err.println(KEY_WRONG_SIZE);
			return null;
		}
		
		//converting cipher text to byteArray
		byte[] byteCiphertext = Base64.getDecoder().decode(strCiphertext);
		
		try {
			//get Cipher Instance
			Cipher cipher = Cipher.getInstance(ALGORITHM_WITH_PADDING);
			
			//initialize Cipher for decryption
			cipher.init(Cipher.DECRYPT_MODE, key, IV);		
			
			//perform decryption
			byte[] bytePlaintext = cipher.doFinal(byteCiphertext);
			
			//convert plain text to string
			String strPlaintext = new String(bytePlaintext, StandardCharsets.UTF_8);
			
			return strPlaintext;
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
}
