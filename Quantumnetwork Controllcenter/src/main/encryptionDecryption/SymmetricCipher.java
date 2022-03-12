package encryptionDecryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
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
	final int KEY_LENGTH;
	/** Simple name of the algorithm used in this class (e.g. "DES", "AES", ...), used for creating SecretKey objects */
	final String TRANSFORMATION_SIMPLE;
	/** Full name of the algorithm used in this class, including mode and padding (e.g. "AES/CBC/PKCS5Padding"), used for Cipher.getInstance() */
	final String TRANSFORMATION_FULL;
	
	/**
	 * Constructor.
	 * @implNote 
	 * Intended to be called in sub-classes with their own constructor,
	 * setting the class fields as appropriate. See {@linkplain AES256} for an example.
	 * @param key_length
	 * 		length of the keys used by this cipher
	 * @param transformation_simple
	 * 		Simple name of the algorithm used in this class (e.g. "DES", "AES", ...), used for creating SecretKey objects
	 * @param transformation_full
	 * 		Full name of the algorithm used in this class, including mode and padding (e.g. "AES/CBC/PKCS5Padding"), used for Cipher.getInstance()
	 */
	protected SymmetricCipher(final int key_length, final String transformation_simple, final String transformation_full) {
		this.KEY_LENGTH = key_length;
		this.TRANSFORMATION_SIMPLE = transformation_simple;
		this.TRANSFORMATION_FULL = transformation_full;
	}
	
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
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the {@linkplain Cipher} implemented in this class
	 * @throws IllegalBlockSizeException
	 * 		if the {@linkplain Cipher} implemented by this class is a block cipher with no padding, 
	 * 		and the plaintext size is not a multiple of the block's size <br>
	 * 		see also documentation of {@linkplain Cipher#doFinal()}
	 */
	public abstract byte[] encrypt(byte[] plaintext, SecretKey key) throws InvalidKeyException, IllegalBlockSizeException;
	
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
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the {@linkplain Cipher} implemented in this class
	 * @throws BadPaddingException
	 * 		if the final block of the ciphertext is not properly padded for the {@linkplain Cipher} implemented in this class <br>
	 * 		see also documentation of {@linkplain Cipher#doFinal()}
	 */
	public abstract byte[] decrypt(byte[] ciphertext, SecretKey key) throws InvalidKeyException, BadPaddingException;
	
	/**
	 * Encrypts a plaintext with the cipher implemented by this class.
	 * @param ciphertext
	 * 		the plaintext to be encrypted <br>
	 * @param key
	 * 		the key to encrypt the plaintext with <br>
	 * 		the first {@link #KEY_LENGTH} bytes will be used as a secret key for the algorithm implemented by this class
	 * @return
	 * 		the ciphertext corresponding to the plaintext being encrypted with the given key
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the {@linkplain Cipher} implemented in this class
	 * @throws IllegalBlockSizeException
	 * 		if the {@linkplain Cipher} implemented by this class is a block cipher with no padding, 
	 * 		and the plaintext size is not a multiple of the block's size <br>
	 * 		see also documentation of {@linkplain Cipher#doFinal()}
	 */
	public byte[] encrypt(byte[] plaintext, byte[] byteKey) throws InvalidKeyException, IllegalBlockSizeException {
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
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the {@linkplain Cipher} implemented in this class
	 * @throws BadPaddingException
	 * 		if the final block of the ciphertext is not properly padded for the {@linkplain Cipher} implemented in this class <br>
	 * 		see also documentation of {@linkplain Cipher#doFinal()}
	 */
	public byte[] decrypt(byte[] ciphertext, byte[] byteKey) throws InvalidKeyException, BadPaddingException {
		return decrypt(ciphertext, byteArrayToSecretKey(byteKey));
	}
	
	/**
	 * Converts a byte array to a SecretKey object for the algorithm implemented in this class.
	 * @param key
	 * 		the byte array to convert <br>
	 * 		must be of the same length as the key (i.e. {@link #getKeyLength()} / 8)
	 * @return
	 * 		a SecretKey object that can be used for encryption and decryption in this class
	 */
	public SecretKey byteArrayToSecretKey(byte[] key) {
		if (key.length * 8 != KEY_LENGTH) 
			throw new IllegalArgumentException(
					"Passed array must be the size of the key used by this algorithm, "
					+ "which is " + getKeyLength() + " bits, or " + KEY_LENGTH / 8 + " bytes.");
		return new SecretKeySpec(key, 0, key.length, TRANSFORMATION_SIMPLE);
	};
	
	/**
	 * @param mode
	 * 		the opmode to initialize the cipher in
	 * @param key
	 * 		key used to initialize the cipher with
	 * @return an instance of the underlying {@linkplain Cipher} used by this encryption algorithm.
	 * 		   this instance will be initialized the same way it would be initialized in this class
	 * @throws InvalidKeyException
	 * 		if the provided key is not a valid key for the {@linkplain Cipher} that this class implements
	 */
	protected abstract Cipher getInitializedInstance(int mode, SecretKey key) throws InvalidKeyException;
	
	/**
	 * Expected key length for the {@linkplain Cipher} implemented by this class, in bits.
	 */
	public final int getKeyLength() {
		return KEY_LENGTH;
	}
}
