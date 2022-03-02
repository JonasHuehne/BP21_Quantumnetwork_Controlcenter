package encryptionDecryption;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for conversion of different inputs into byte arrays and Secret Keys
 * 
 * @author Lukas Dentler, Sasha Petri
 */
public class CryptoUtility {
	//constants
	private final static int BIT_STRING_LENGTH_AES256 = 256;
	private final static int BYTE_ARRAY_LENGTH_AES256 = 32;
	private final static String ALGORITHM_AES = "AES";
	private final static String INVALID_KEY_ERR = "An invalid key was used. Please use a key of length 256";
	
	/**
	 * Used to convert a bit string containing 256 bits into an byteArray containing 32 bytes
	 * 
	 * @param str A string consisting of 256 characters, each one being either 0 or 1
	 * @return A byteArray containing 32 bytes corresponding to the 256 bits of str
	 * 		   returns null if str is not fulfilling the contracted specification.
	 * @throws NumberFormatException if str contains any other char besides 0 or 1
	 */
	public static byte[] bitString256ToByteArray32(String str) throws NullPointerException, NumberFormatException {
		
		//initialize local variables
		byte[] bytes = new byte[32];
		int j = 0;
		
		//checking that str has the right length
		if (str.length() != BIT_STRING_LENGTH_AES256) {
			throw new IllegalArgumentException("Bit string must have a length of " + BIT_STRING_LENGTH_AES256 + " but was " + str.length());
		}
		
		//converting bits from bitString into bytes that can be put into a byteArray
		for(int i=0; i < 32; i++) {
			bytes[j] = (byte) (Integer.valueOf(str.substring(i * 8, i * 8 + 8), 2) - 128);
			j++;
		}
		
		return bytes;
	}
	
	/**
	 * Used to convert a String with key data to a SecretKey object usable with javax.crypto.Cipher
	 * 
	 * @param str a bitString with 256 bits
	 * @return An instance of SecretKey used to encrypt or decrypt with javax.crypto.Cipher
	 * 		   returns null if str is not fulfilling the contracted specification.
	 * @throws NumberFormatException if str contains any other char besides 0 or 1

	 */
	public static SecretKey stringToSecretKeyAES256(String str) {
		//checking that str has the right length
		if (str.length() != BIT_STRING_LENGTH_AES256) {
			throw new IllegalArgumentException("Expected length of string key argument to be " + BIT_STRING_LENGTH_AES256 + " but received a string with length " + str.length());
		}
		//converting String to byte array
		byte [] byteKey = bitString256ToByteArray32(str);
		//wrapping key information in SecretKey class for AES 
		return new SecretKeySpec(byteKey, 0, byteKey.length, ALGORITHM_AES);
	}
	
	/**
	 * Used to convert a byte array with key data to a SecretKey object usable with javax.crypto.Cipher
	 * 
	 * @param bytes a byte array with 256 bits (32 bytes) 
	 * @return An instance of SecretKey used to encrypt or decrypt with javax.crypto.Cipher
	 * 		   returns null if bytes is not fulfilling the contracted specification.
	 */
	public static SecretKey byteArrayToSecretKeyAES256(byte[] bytes) {
		if (bytes.length != BYTE_ARRAY_LENGTH_AES256) {
			throw new IllegalArgumentException("Expected length of key byte array argument to be " + BYTE_ARRAY_LENGTH_AES256 + " but received an array with length " + bytes.length);
		}
		//wrapping key information in SecretKey class for AES
		return new SecretKeySpec(bytes, 0, bytes.length,ALGORITHM_AES);
	}
	
	// TODO: For large files, these methods will likely use a lot of RAM.
	// Divide & Conquer + change encrypt to instead modify the array parsed to it (no new malloc)? 
	
	/**
	 * Reads a file and encrypts it with the {@link AES256} crypto module, using the given key.
	 * @param fileToEncrypt
	 * 		path to a file to encrypt
	 * @param key
	 * 		the AES256 secret key to encrypt the file with
	 * @return
	 * 		the bytes of the file, encrypted with the given secret key
	 * @throws IOException
	 * 		if a problem occured trying to read the file
	 */
	public static byte[] loadAndEncryptFile_AES256(Path fileToEncrypt, SecretKey key) throws IOException {
		byte[] bytesToEncrypt = Files.readAllBytes(fileToEncrypt);
		return AES256.encrypt(bytesToEncrypt, key);
	}
	
	/**
	 * Wrapper method for {@linkplain #loadAndEncryptFile_AES256(Path, SecretKey)}.
	 * Calls {@linkplain #loadAndEncryptFile_AES256(Path, SecretKey)} with the given byte array passed as an AES256 SecretKey object.
	 * @param fileToEncrypt
	 * 		the file to encrypt 
	 * @param key
	 * 		size 32 array (256 bit), will be transformed into an AES256 SecretKey object
	 * @return
	 * 		the bytes of the file, encrypted with the given secret key
	 * @throws IOException
	 * 		if a problem occured trying to read the file
	 */
	public static byte[] loadAndEncryptFile_AES256(Path fileToEncrypt, byte[] key) throws IOException {
		SecretKey sk = byteArrayToSecretKeyAES256(key);
		return loadAndEncryptFile_AES256(fileToEncrypt, sk);
	}
	
	/**
	 * Decrypts a file and saves it to the specified location.
	 * @param fileBytes
	 * 		bytes of the file to decrypt
	 * @param key
	 * 		the AES256 secret key to decrypt the file with
	 * @param directory
	 * 		directory to save the file in, must exist
	 * @param fileName
	 * 		what the file should be saved as
	 * @throws NotDirectoryException
	 * 		if {@code directory} does not specify a directory
	 * @throws FileNotFoundException
	 * 		if {@code directory} does not resolve to an existing directory
	 * @throws IOException
	 * 		other IOExceptions may be thrown if the writing of the file fails
	 */
	public static void decryptAndSaveFile_AES256(byte[] fileBytes, SecretKey key, Path directory, String fileName) throws IOException {
		if(!Files.isDirectory(directory)) throw new NotDirectoryException("Path " + directory.toString() + " does not specify a directory.");
		if(!Files.exists(directory)) throw new FileNotFoundException("Path " + directory.toString() + " did not resolve to an existing directory.");
		byte[] decrBytes = AES256.decrypt(fileBytes, key);
		
		Files.write(directory.resolve(fileName), decrBytes);
	}
	
	/**
	 * Wrapper Method for {@linkplain #decryptAndSaveFile_AES256(byte[], SecretKey, Path, String)}.
	 * Calls that method with the byte array passed as an AES256 SecretKey object.
	 * @param fileBytes
	 * 		bytes of the file to decrypt
	 * @param key
	 * 		size 32 array (256 bit), will be transformed into an AES256 SecretKey object
	 * @param directory
	 * 		directory to save the file in, must exist
	 * @param fileName
	 * 		what the file should be saved as
	 * @throws NotDirectoryException
	 * 		if {@code directory} does not specify a directory
	 * @throws FileNotFoundException
	 * 		if {@code directory} does not resolve to an existing directory
	 * @throws IOException
	 * 		if {@code directory} does not resolve to an existing directory 
	 */
	public static void decryptAndSaveFile_AES256(byte[] fileBytes, byte[] key, Path directory, String fileName) throws IOException {
		SecretKey sk = byteArrayToSecretKeyAES256(key);
		decryptAndSaveFile_AES256(fileBytes, sk, directory, fileName);
	}
}
