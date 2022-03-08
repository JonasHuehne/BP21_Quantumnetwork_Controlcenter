package encryptionDecryption;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This abstract super-class provides common function for different symmetric ciphers such as AES256 that 
 * can be used interchangeably in this program, for the purpose of encrypting data with the generated secret keys. <br>
 * Implementations are currently expected to use {@linkplain Cipher}s. 
 * @author Sasha Petri
 */
public abstract class SymmetricCipher {
	
	/*
	 * For information on transformations, see https://docs.oracle.com/javase/9/docs/api/javax/crypto/Cipher.html
	 */
	
	/** Expected length of the key (in bits) used in the algorithm implemented by this class */
	private final int KEY_LENGTH = 0;
	/** Simple name of the algorithm used in this class (e.g. "DES", "AES", ...), used for creating SecretKey objects */
	private final String TRANSFORMATION_SIMPLE = "";
	/** Full name of the algorithm used in this class, including mode and padding (e.g. "AES/CBC/PKCS5Padding"), used for Cipher.getInstance() */
	private final String TRANSFORMATION_FULL = "";
	
	public SymmetricCipher() {
		
	};
	
	/**
	 * Encrypts a plaintext with the cipher implemented by this class.
	 * @param plaintext
	 * 		the plaintext to be encrypted <br>
	 * @param key
	 * 		the key to encrypt the plaintext with <br>
	 * 		must be a valid key for the crypto algorihm implemented by this class,
	 * 		e.g. for AES256 it must be a SecretKey object associated with the AES algorithm
	 * 		and with a key length of 256 bits
	 * @return
	 * 		the ciphertext corresponding to the plaintext being encrypted with the given key
	 */
	public abstract byte[] encrypt(byte[] ciphertext, SecretKey key);
	
	/**
	 * Decrypts a ciphertext with the cipher implemented by this class.
	 * @param ciphertext
	 * 		the ciphertext to be decypted <br>
	 * @param key
	 * 		the key to decrypt the ciphertext with <br>
	 * 		must be a valid key for the crypto algorihm implemented by this class,
	 * 		e.g. for AES256 it must be a SecretKey object associated with the AES algorithm
	 * 		and with a key length of 256 bits
	 * @return
	 * 		the plaintext corresponding to the ciphertext being decrypted with the given key
	 */
	public abstract byte[] decrypt(byte[] ciphertext, SecretKey key);
	
	/**
	 * Encrypts a plaintext with the cipher implemented by this class.
	 * @param ciphertext
	 * 		the plaintext to be encrypted <br>
	 * @param key
	 * 		the key to encrypt the plaintext with <br>
	 * 		the first {@link #KEY_LENGTH} bytes will be used as a secret key for the algorithm implemented by this class
	 * @return
	 * 		the ciphertext corresponding to the plaintext being encrypted with the given key
	 */
	public byte[] encrypt(byte[] plaintext, byte[] byteKey) {
		return encrypt(plaintext, byteArrayToSecretKey(byteKey));
	}
	
	/**
	 * Decrypts a ciphertext with the cipher implemented by this class.
	 * @param ciphertext
	 * 		the ciphertext to be decypted <br>
	 * @param key
	 * 		the key to decrypt the ciphertext with <br>
	 * 		the first {@link #KEY_LENGTH} bytes will be used as a secret key for the algorithm implemented by this class
	 * @return
	 * 		the plaintext corresponding to the ciphertext being decrypted with the given key
	 */
	public byte[] decrypt(byte[] ciphertext, byte[] byteKey) {
		return decrypt(ciphertext, byteArrayToSecretKey(byteKey));
	}
	
	/**
	 * Converts a byte array to a SecretKey object for the algorithm implemented in this class.
	 * @param key
	 * 		the byte array to convert
	 * @return
	 * 		a SecretKey object that can be used for encryption and decryption in this class
	 */
	private SecretKey byteArrayToSecretKey(byte[] key) {
		return new SecretKeySpec(key, 0, KEY_LENGTH, TRANSFORMATION_SIMPLE);
	};
	
	/**
	 * @return an instance of the underlying {@linkplain Cipher} used by this encryption algorithm.
	 */
	protected Cipher getInstance() {
		try {
			return Cipher.getInstance(TRANSFORMATION_FULL);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new RuntimeException("This method has not been implemented correctly. "
					+ "The full transformation does not correspond to a valid algorithm / algorithm with padding.");
		}
	}
	
	
}
