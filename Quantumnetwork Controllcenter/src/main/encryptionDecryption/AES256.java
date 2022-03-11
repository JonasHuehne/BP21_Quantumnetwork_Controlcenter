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


/**
 * Class for AES-256 encryption and decryption of Strings using the CBC Mode with a constant IV
 * 
 * @author Lukas Dentler, Sasha Petri
 */
public class AES256 extends SymmetricCipher {
	
	//generating constant IV
	private static final byte[] BYTE_IV = {42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42};
	private static final IvParameterSpec IV = new IvParameterSpec(BYTE_IV);
	
	/**
	 * Constructor.
	 */
	public AES256() {
		super(256, "AES", "AES/CBC/PKCS5Padding");
	}
	
	@Override
	public byte[] encrypt(byte[] plaintext, SecretKey key) throws InvalidKeyException, IllegalBlockSizeException {
		if (plaintext == null || key == null) throw new NullPointerException();
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
		if (ciphertext == null || key == null) throw new NullPointerException();
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
