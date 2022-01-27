package test;

import main.encryptionDecryption.AES256;
import main.encryptionDecryption.CryptoUtility;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;


public class EncryptDecryptTests {
	String original = "This is a Test!\\\"§$%&/()=?öäü to be fair a very long test text to see if it really works. So i am just going to smash my head on the keyboard a bit more: ajodaohglaenkohadoibhlaknowehojobhaskdjnfoaishcvon'#*_-:.,,;<>|adasogphopaidfhgvpoiiruhgaowenklödaioühoigüoh";
	String bitStringKey = "0101010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyWithChars = "abc1010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyGreaterOne = "3011010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyShort = "01110101";
	String bitStringKeyLong = "0101010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111110101100011111010101110101110101110101110111101111000001111111010101010111101111";

	private final PrintStream standardOut = System.out;
	private final PrintStream standardErr = System.err;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	private final ByteArrayOutputStream outputErrCaptor = new ByteArrayOutputStream();
	
	private final static String KEY_WRONG_SIZE = "An invalid key was used. Please use a key of length 256";
	private final static String BIT_STRING_WRONG_LENGTH = "Please only use 256 char long bit strings";
	private final static String KEYGEN_ERROR = "An ERROR occured during the keygeneration:";
	private final static String ALGORITHM_AES = "AES";
	
	
	@Test
	/*
	 * Testing encryption and decryption with a bit string as Key
	 */
	public void testEncryptionDecryptionBitStringKey(){
		String encrypted = AES256.encrypt(original, bitStringKey);
		String decrypted = AES256.decrypt(encrypted, bitStringKey);
			
		assertNotEquals(original,encrypted);
		assertNotEquals(encrypted,decrypted);
		assertEquals(decrypted,original);
	}
	
	@Test
	/*
	 * Testing encryption and decryption with a generated Key
	 */
	public void testEncryptionDecryptionGeneratedKey() {
		SecretKey key = null;
		
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
			keyGen.init(256);
			key = keyGen.generateKey();
		}
		catch (Exception e) {
			System.out.println(KEYGEN_ERROR +e.toString());
		}		
		
		String encrypted = AES256.encrypt(original, key);
		String decrypted = AES256.decrypt(encrypted, key);
		
		assertNotEquals(original,encrypted);
		assertNotEquals(encrypted,decrypted);
		assertEquals(decrypted,original);
	}	
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Chars during encryption
	 */
	public void testEncryptionBitStringContainsChars() {		
		assertThrows(NumberFormatException.class,() -> {
			assertNull(AES256.encrypt(original, bitStringKeyWithChars));
			});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Numbers greater 1 during encryption
	 */
	public void testEncryptionBitStringGreaterOne() {
		assertThrows(NumberFormatException.class,() -> {
			assertNull(AES256.encrypt(original, bitStringKeyGreaterOne));
			});
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a too short bit string during encryption
	 */
	public void testEncryptionBitStringTooShort() {
		System.setErr(new PrintStream(outputErrCaptor));
		
		assertNull(AES256.encrypt(original, bitStringKeyShort));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a too long bit string during encryption
	 */
	public void testEncryptionBitStringTooLong() {
		System.setErr(new PrintStream(outputErrCaptor));
		
		assertNull(AES256.encrypt(original, bitStringKeyLong));	
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a generated key of wrong length during encryption
	 */
	public void testEncryptionGeneratedKeyWrongLength() {
		System.setErr(new PrintStream(outputErrCaptor));
		SecretKey keyWrongLength = null;
		
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
			keyGen.init(128);
			keyWrongLength = keyGen.generateKey();
		}
		catch (Exception e) {
			System.out.println(KEYGEN_ERROR +e.toString());
		}
		
		assertNull(AES256.encrypt(original, keyWrongLength));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as key
	 */
	public void testEncryptionNullStrKey() {
		assertThrows(NullPointerException.class,() -> {
			assertNull(AES256.encrypt(original,(String) null));
			});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as key
	 */
	public void testEncryptionNullSecretKey() {
		SecretKey sk = null;
		
		assertThrows(NullPointerException.class,() -> {
			assertNull(AES256.encrypt(original,sk));
			});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as plaintext with a valid key
	 */
	public void testEncryptionNullPlaintext() {
		assertThrows(NullPointerException.class, () -> {
			assertNull(AES256.encrypt(null, bitStringKey));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as plaintext and key
	 */
	public void testEncryptionNullPlaintextStrKey() {
		assertThrows(NullPointerException.class, () -> {
			assertNull(AES256.encrypt(null,(String) null));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as plaintext and key
	 */
	public void testEncryptionNullPlaintextSecretKey() {
		assertThrows(NullPointerException.class, () -> {
			SecretKey sk = null;
			assertNull(AES256.encrypt(null,sk));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Chars during decryption
	 */
	public void testDecryptionBitStringContainsChars() {
		String encrypted = AES256.encrypt(original, bitStringKey);
		
		assertThrows(NumberFormatException.class,() -> {
			assertNull(AES256.decrypt(encrypted, bitStringKeyWithChars));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Numbers greater 1 during decryption
	 */
	public void testDecryptionBitStringGreaterOne() {
		String encrypted = AES256.encrypt(original, bitStringKey);
		
		assertThrows(NumberFormatException.class,() -> {
			assertNull(AES256.decrypt(encrypted, bitStringKeyGreaterOne));
			});
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a too short bit string during decryption
	 */
	public void testDecryptionBitStringTooShort() {
		System.setErr(new PrintStream(outputErrCaptor));
		String encrypted = AES256.encrypt(original, bitStringKey);
		
		assertNull(AES256.decrypt(encrypted, bitStringKeyShort));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	

	@Test
	/*
	 * Testing that correct Error message is shown when using a too long bit string during decryption
	 */
	public void testDecryptionBitStringTooLong() {
		System.setErr(new PrintStream(outputErrCaptor));
		String encrypted = AES256.encrypt(original, bitStringKey);
		
		assertNull(AES256.decrypt(encrypted, bitStringKeyLong));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	

	@Test
	/*
	 * Testing that correct Error message is shown when using a generated key of wrong length during decryption
	 */
	public void testDecryptionGeneratedKeyWrongLength() {
		System.setErr(new PrintStream(outputErrCaptor));
		SecretKey keyEnc = null;
		SecretKey keyWrongLength = null;
		
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
			keyGen.init(256);
			keyEnc = keyGen.generateKey();
		}
		catch (Exception e) {
			System.out.println(KEYGEN_ERROR +e.toString());
		}
				
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
			keyGen.init(128);
			keyWrongLength = keyGen.generateKey();
		}
		catch (Exception e) {
			System.out.println(KEYGEN_ERROR +e.toString());
		}
			
		String encrypted = AES256.encrypt(original, keyEnc);
		
		assertNull(AES256.decrypt(encrypted, keyWrongLength));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}

	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as key during decryption
	 */
	public void testDecryptionNullStrKey() {
		String encrypted = AES256.encrypt(original, bitStringKey);

		assertThrows(NullPointerException.class,() -> {
			assertNull(AES256.decrypt(encrypted,(String) null));
			});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as key during decryption
	 */
	public void testDecryptionNullSecretKey() {
		String encrypted = AES256.encrypt(original, bitStringKey);
		SecretKey sk = null;
		
		assertThrows(NullPointerException.class,() -> {
			assertNull(AES256.decrypt(encrypted,sk));
			});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as cipher text with a valid key
	 */
	public void testDecryptionNullPlaintext() {
		assertThrows(NullPointerException.class, () -> {
			assertNull(AES256.decrypt(null, bitStringKey));
		});
		
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as plaintext and key
	 */
	public void testDecryptionNullPlaintextStrKey() {
		assertThrows(NullPointerException.class, () -> {
			assertNull(AES256.decrypt(null,(String) null));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null is used as plaintext and key
	 */
	public void testDecryptionNullPlaintextSecretKey() {
		assertThrows(NullPointerException.class, () -> {
			SecretKey sk = null;
			assertNull(AES256.decrypt(null,sk));
		});
	}
	
	@Test
	/*
	 *Testing the 256 char bit string to 32 bytes array method 
	 *with extreme cases:
	 *max value
	 *min value
	 *zero
	 *and random values between 
	 */
	public void testBitString256ToByteArray32() {
		String bitString = "0000000011111111100000001011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
		byte[] byteArray = CryptoUtility.bitString256ToByteArray32(bitString);
		assertEquals(byteArray[0], -128);
		assertEquals(byteArray[1], 127);
		assertEquals(byteArray[2], 0);
	}
	
	@Test
	/*
	 * Testing the 256 char bit string to 32 bytes array method when using
	 * too long bit string
	 */
	public void testBitString256ToByteArray32TooLong() {
		System.setOut(new PrintStream(outputStreamCaptor));
		
		assertNull(CryptoUtility.bitString256ToByteArray32(bitStringKeyLong));
		assertEquals(BIT_STRING_WRONG_LENGTH, outputStreamCaptor.toString().trim());
		
		System.setOut(standardOut);
	}
	
	@Test
	/*
	 * Testing the 256 char bit string to 32 bytes array method when using
	 * too short bit string
	 */
	public void testBitString256ToByteArray32TooShort() {
		System.setOut(new PrintStream(outputStreamCaptor));
		
		assertNull(CryptoUtility.bitString256ToByteArray32(bitStringKeyShort));
		assertEquals(BIT_STRING_WRONG_LENGTH, outputStreamCaptor.toString().trim());
		
		System.setOut(standardOut);
	}
	
	@Test
	/*
	 * Testing the 256 char bit string to 32 bytes array method when using 
	 * null as bit string
	 */
	public void testBitString256ToByteArray32Null() {
		assertThrows(NullPointerException.class,() -> {
			assertNull(CryptoUtility.bitString256ToByteArray32(null));
		});
		
	}
	
	@Test
	/*
	 * Testing the 256 char bit string to 32 bytes array method when using
	 * chars in bit string
	 */
	public void testBitString256ToByteArray32Chars() {
		assertThrows(NumberFormatException.class,()->{
			assertNull(CryptoUtility.bitString256ToByteArray32(bitStringKeyWithChars));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Chars
	 */
	public void testStringToSecretKeyAES256BitStringContainsChars() {		
		assertThrows(NumberFormatException.class,() -> {
			assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyWithChars));
		});
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown, when bit string for key contains Chars
	 */
	public void testStringToSecretKeyAES256BitStringGreaterOne() {		
		assertThrows(NumberFormatException.class,() -> {
			assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyGreaterOne));
		});
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a too short bit string
	 */
	public void testStringToSecretKeyAES256BitStringTooShort() {
		System.setErr(new PrintStream(outputErrCaptor));
		
		assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyShort));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	
	@Test
	/*
	 * Testing that correct Error message is shown when using a too long bit string
	 */
	public void testStringToSecretKeyAES256BitStringTooLong() {
		System.setErr(new PrintStream(outputErrCaptor));
		
		assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyLong));
		assertEquals(KEY_WRONG_SIZE, outputErrCaptor.toString().trim());
		
		System.setErr(standardErr);
	}
	
	@Test
	/*
	 * Testing that correct Exception is thrown when null used as String
	 */
	public void testStringToSecretKeyAES256Null() {
		assertThrows(NullPointerException.class, () -> {
			assertNull(CryptoUtility.stringToSecretKeyAES256(null));
		});
	}
	
	@Test
	/*
	 * Testing that SecretKey object contains the correct key information.
	 */
	public void testStringToSecretKeyAES256ValidBitString() {
		SecretKey sk = CryptoUtility.stringToSecretKeyAES256(bitStringKey);
		byte[] bytes = CryptoUtility.bitString256ToByteArray32(bitStringKey);
		byte[] keyBytes = sk.getEncoded();
		for(int i = 0; i < 32; i++) {
			assertEquals(bytes[i], keyBytes[i]);
		}
	}
}
