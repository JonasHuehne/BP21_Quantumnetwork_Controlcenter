package encryptionDecryption;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;


/**
 * Class for AES-256 encryption and decryption of Strings using the CBC Mode with a constant IV
 * 
 * @author Lukas Dentler, Sasha Petri
 */
public class AES256 extends SymmetricCipher {
	
	//generating constant IV
	private static final byte[] BYTE_IV = {42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42};
	private static final IvParameterSpec IV = new IvParameterSpec(BYTE_IV);

	//constants
	private static final String ALGORITHM_WITH_PADDING = "AES/CBC/PKCS5Padding";
	private static final int KEY_LENGTH_BYTE = 32;
	private static final int KEY_LENGTH_BIT = 256;
	private static final String KEY_WRONG_SIZE = "An invalid key was used. Please use a key of length 256";
	private static final Log log = new Log(AES256.class.getName(), LogSensitivity.WARNING);

	/**
	 * Constructor.
	 */
	public AES256() {
		super(256, "AES", "AES/CBC/PKCS5Padding");
	}
	
	@Override
	public byte[] encrypt(byte[] plaintext, SecretKey key) throws InvalidKeyException, IllegalBlockSizeException {
		Cipher c = getInitializedInstance(Cipher.ENCRYPT_MODE, key);
		try {
			return c.doFinal(plaintext);
		} catch (IllegalBlockSizeException e) {
			// In practicality this is never thrown, due to us using padding
			throw e;
		} catch (BadPaddingException e) {
			// Never thrown, since cipher is initialized in encrypt mode, see documentation of Cipher class
			throw new RuntimeException("Implementation error, this Exception should not have been thrown.");
		}
	}
	
	@Override
	public byte[] decrypt(byte[] ciphertext, SecretKey key) throws InvalidKeyException, BadPaddingException {
		Cipher c = getInitializedInstance(Cipher.DECRYPT_MODE, key);
		try {
			return c.doFinal(ciphertext);
		} catch (BadPaddingException e) {
			throw e;
		} catch (IllegalBlockSizeException e) {
			// Never thrown, since cipher is initialized in encrypt mode, see documentation of Cipher class
			throw new RuntimeException("Implementation error, this Exception should not have been thrown.");
		}
	}
	
	@Override
	protected Cipher getInitializedInstance(int mode, SecretKey key) throws InvalidKeyException {
		Cipher c;
		
		try {
			c = Cipher.getInstance(TRANSFORMATION_FULL);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			// Occurs only if the transformations specified in the constructor are wrong, i.e. a programming mistake
			throw new RuntimeException(
					"ERROR - A " + e1.getClass().getSimpleName() + " occurred. "
					+ "This is an implementation error. Check the Constructor of this class."
			);
		}
		
		try {
			c.init(mode, key, IV);
		} catch (InvalidKeyException e) {
			throw e;
		} catch (InvalidAlgorithmParameterException e) {
			// In this case, this only occurs if the IV is not valid, which is again a programming mistake
			throw new RuntimeException(
					"ERROR - A " + e.getClass().getSimpleName() + " occurred. "
					+ "This is an implementation error, most likely cause by a problem with the IV."
			);
		}
		
		return c;
	}

	
	
}
