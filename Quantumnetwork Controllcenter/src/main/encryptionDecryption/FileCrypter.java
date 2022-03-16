package encryptionDecryption;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

/**
 * Used for the encryption and decryption of files.
 * @author Sasha Petri
 */
public class FileCrypter {
	
	/** Buffer size in byte used for file encryption and decryption */
	public final static int BUFFERSIZE = 8192;

	/**
	 * Loads, encrypts and saves a file using the given cipher and key.
	 * @param toDecrypt
	 * 		the file to encrypt
	 * @param cipher
	 * 		the cipher to encrypt the file with 
	 * @param key
	 * 		the key to encrypt the file with
	 * @param outPath
	 * 		path to the output file that is to be written <br>
	 * 		must be different than the path of the input file
	 * @return
	 * 		the encrypted file
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the algorithm implemented by the provided {@linkplain SymmetricCipher}
	 * @throws IllegalBlockSizeException
	 * 		if the algorithm is a block cipher with no padding, and the input file's size is not a multiple of the block's size,
	 * 		see also documentation of {@linkplain Cipher#doFinal()}
	 * @throws IOException
	 * 		if an I/O Exception occurred trying to read the file to be decrypted, or write the decrypted file
	 */
	public static File encryptAndSave(File toEncrypt, SymmetricCipher cipher, SecretKey key, Path outPath) 
			throws InvalidKeyException, IllegalBlockSizeException, IOException {
		try {
			return encryptDecryptAndSave(toEncrypt, cipher, key, outPath, Cipher.ENCRYPT_MODE);
		} catch (BadPaddingException e) { 
			// Never thrown, see documentation
			throw new RuntimeException("Code error - this type of Exception should never be thrown here.");
		}catch (InvalidKeyException | IllegalBlockSizeException | IOException e) {
			throw e;
		}
	}
	
	/**
	 * Loads, decrypts and saves a file using the given cipher and key.
	 * @param toDecrypt
	 * 		the file to decrypt
	 * @param cipher
	 * 		the cipher to decrypt the file with 
	 * @param key
	 * 		the key to decrypt the file with
	 * @param outPath
	 * 		path to the output file that is to be written <br>
	 * 		must be different than the path of the input file
	 * @return
	 * 		the decrypted file
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the algorithm implemented by the provided {@linkplain SymmetricCipher}
	 * @throws BadPaddingException
	 * 		if the final block of the input file is not properly padded for the algorithm implemented by the provided
	 * 		{@linkplain SymmetricCipher}, see also documentation of {@linkplain Cipher#doFinal()}
	 * @throws IOException
	 * 		if an I/O Exception occurred trying to read the file to be decrypted, or write the decrypted file
	 */
	public static File decryptAndSave(File toDecrypt, SymmetricCipher cipher, SecretKey key, Path outPath) 
			throws InvalidKeyException, BadPaddingException, IOException {
		try {
			return encryptDecryptAndSave(toDecrypt, cipher, key, outPath, Cipher.DECRYPT_MODE);
		} catch (IllegalBlockSizeException e) {
			// Never thrown, see documentation
			throw new RuntimeException("Code error - this type of Exception should never be thrown here.");
		} catch (BadPaddingException | InvalidKeyException | IOException e) {
			throw e;
		}
	}
	
	/**
	 * Encrypts / decrypts a file with the given key.
	 * @implNote Implementation is an adjusted version of a code snippet by Joop Eggen, 
	 * which can be found here https://stackoverflow.com/questions/49235427/java-out-of-memory-error-during-encryption.
	 * @param input
	 * 		the file to be encrypted / decrypted
	 * @param sc
	 * 		the cipher to be used
	 * @param key
	 * 		the key to be used for encryption / decryption
	 * @param outPath
	 * 		path to the output file that is to be written <br>
	 * 		must be different than the path of the input file
	 * @param mode
	 * 		{@value Cipher#ENCRYPT_MODE} for encryption, {@value Cipher#DECRYPT_MODE} for decryption
	 * @return
	 * 		the File corresponding to the encrypted / decrypted file
	 * @throws InvalidKeyException
	 * 		if {@code key} is not a valid key for the cipher implemented by {@code sc}
	 * @throws IOException 
	 * 		if an I/O Exception occurred trying to read or write the file
	 * @throws BadPaddingException 
	 * 		thrown only in decryption mode, for details see {@linkplain Cipher#doFinal()}
	 * @throws IllegalBlockSizeException 
	 * 		thrown only in encryption mode, for details see {@linkplain Cipher#doFinal()}
	 */
	private static File encryptDecryptAndSave(File input, SymmetricCipher sc, SecretKey key, Path outPath, int mode) 
			throws InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
		if (input.toPath().equals(outPath)) throw new IllegalArgumentException("Path of output file may not be the same as path of input file.");
		
		if (mode != Cipher.ENCRYPT_MODE && mode != Cipher.DECRYPT_MODE) 
			throw new IllegalArgumentException("Mode must be either Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE.");
		
		Cipher c = sc.getInitializedInstance(mode, key);
		
		byte[] buffer = new byte[BUFFERSIZE];
		
		try (InputStream fileReader = Files.newInputStream(input.toPath()); // try-with-resources
			 OutputStream fileWriter = Files.newOutputStream(outPath)) {
			
			// InputStream.read(buffer) reads as many bytes as it can from the file until file end
			// and returns how many could be read. -1 indicates there is no more data (end of file)
			int bytesRead;
			while ((bytesRead = fileReader.read(buffer)) > 0) {
				byte[] cryptedBytes = c.update(buffer, 0, bytesRead);
				fileWriter.write(cryptedBytes);
			}
			byte[] finalCryptedBytes = c.doFinal();
			fileWriter.write(finalCryptedBytes);
		}
		return outPath.toFile();
	}
}
