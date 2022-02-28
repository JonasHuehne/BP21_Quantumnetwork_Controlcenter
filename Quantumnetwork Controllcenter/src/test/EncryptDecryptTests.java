import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import encryptionDecryption.AES256;
import encryptionDecryption.CryptoUtility;

/**
 * Tests for Classes of the encryptionDecryption package.
 * @author Sasha Petri, Lukas Dentler
 */
class EncryptDecryptTests {
	String original = "This is a Test!\\\"§$%&/()=?öäü to be fair a very long test text to see if it really works. So i am just going to smash my head on the keyboard a bit more: ajodaohglaenkohadoibhlaknowehojobhaskdjnfoaishcvon'#*_-:.,,;<>|adasogphopaidfhgvpoiiruhgaowenklödaioühoigüoh";
	
	
	String bitStringKey = "0101010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyWithChars = "abc1010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyGreaterOne = "3011010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	String bitStringKeyShort = "01110101";
	String bitStringKeyLong = "0101010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111110101100011111010101110101110101110101110111101111000001111111010101010111101111";
	byte[] byteKey = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32}; 
	byte [] byteKeyLong = new byte [] { (byte) 0,  (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};
	byte [] byteKeyShort = new byte[] { (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15, (byte) 16, (byte) 17, (byte) 18, (byte) 19, (byte) 20, (byte) 21, (byte) 22, (byte) 23, (byte) 24, (byte) 25, (byte) 26, (byte) 27, (byte) 28, (byte) 29, (byte) 30, (byte) 31, (byte) 32};
	
	String plainText 	= "";
	
	String keyBase64 	= "";
	String keyBitString = "";
	
	byte [] bytePlainText  = plainText.getBytes();
	
	
	
	private final PrintStream standardOut = System.out;
	private final PrintStream standardErr = System.err;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	private final ByteArrayOutputStream outputErrCaptor = new ByteArrayOutputStream();
	
	private final static String KEY_WRONG_SIZE = "An invalid key was used. Please use a key of length 256";
	private final static String BIT_STRING_WRONG_LENGTH = "Please only use 256 char long bit strings";
	private final static String KEYGEN_ERROR = "An ERROR occured during the keygeneration:";
	private final static String ALGORITHM_AES = "AES";
	
	
	@Nested
	class AES256_Regular_Cases {
		
		@Nested
		class Methods_Using_SecretKey {
			
			@Test
			public void encrypt_decrypt_works_for_byte_arrays_with_valid_secret_key() {
				SecretKey key = null;
				
				try{
					KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
					keyGen.init(256);
					key = keyGen.generateKey();
				}
				catch (Exception e) {
					System.out.println(KEYGEN_ERROR +e.toString());
				}		
				
				byte[] encryptedBytes = AES256.encrypt(bytePlainText, key);
				byte[] decryptedBytes = AES256.decrypt(encryptedBytes, key);
				
				assertNotEquals(original,encryptedBytes);
				assertNotEquals(encryptedBytes,decryptedBytes);
				assertArrayEquals(decryptedBytes,bytePlainText);
			}
			
			@Test
			public void encrypt_decrypt_works_for_Strings_with_valid_secret_key() {
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
			
		}
		
		@Nested
		class Wrapper_Methods {
			@Test
			/*
			 * Testing encryption and decryption with a bit string as Key
			 */
			public void wrapper_methods_using_bit_string_as_key_work_with_fixed_key(){
				String encrypted = AES256.encrypt(original, bitStringKey);
				String decrypted = AES256.decrypt(encrypted, bitStringKey);
					
				assertNotEquals(original,encrypted);
				assertNotEquals(encrypted,decrypted);
				assertEquals(decrypted,original);
			}
			
			@Test
			/*
			 * Testing encryption and decryption with a byte array as Key
			 */
			public void wrapper_methods_using_byte_array_as_key_work_with_fixed_key(){
				String encrypted = AES256.encrypt(original, byteKey);
				String decrypted = AES256.decrypt(encrypted, byteKey);
					
				assertNotEquals(original,encrypted);
				assertNotEquals(encrypted,decrypted);
				assertEquals(decrypted,original);
			}
		}
	}
	
	@Nested
	class AES256_Edge_Cases {
		
		@Test
		public void encrypt_decrypt_for_empty_array() {
			SecretKey key = null;
			
			try{
				KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
				keyGen.init(256);
				key = keyGen.generateKey();
			}
			catch (Exception e) {
				System.out.println(KEYGEN_ERROR +e.toString());
			}		
			
			byte[] encryptedBytes = AES256.encrypt(new byte[] {}, key);
			byte[] decryptedBytes = AES256.decrypt(encryptedBytes, key);
			
			assertArrayEquals(decryptedBytes,new byte[] {});

		}
		
		@Test
		public void encrypt_decrypt_for_empty_string() {
			SecretKey key = null;
			
			try{
				KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
				keyGen.init(256);
				key = keyGen.generateKey();
			}
			catch (Exception e) {
				System.out.println(KEYGEN_ERROR +e.toString());
			}		
			
			String encryptedEmptyString = AES256.encrypt("", key);
			String decryptedEmptyString = AES256.decrypt(encryptedEmptyString, key);

			assertEquals(decryptedEmptyString, "");
		}
		
		
		@Test
		public void ILA_encrypt_byte_arr_wrong_length() {
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.encrypt(original, byteKeyLong);
			});
			
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.encrypt(original, byteKeyShort);
			});
		}
		
		@Test
		public void ILA_decrypt_byte_arr_wrong_length() {
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.decrypt(original, byteKeyLong);
			});
			
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.decrypt(original, byteKeyShort);
			});
		}
		
		@Test
		public void ILA_encrypt_key_string_wrong_length() {
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.encrypt(original, bitStringKeyLong);
			});
			
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.encrypt(original, bitStringKeyShort);
			});
		}
		
		@Test
		public void ILA_decrypt_key_string_wrong_length() {
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.decrypt(original, bitStringKeyLong);
			});
			
			assertThrows(IllegalArgumentException.class, () -> 
			{
				AES256.decrypt(original, bitStringKeyShort);
			});
		}
		
		@Test
		public void NFE_decrypt_key_string_wrong_chars() {
			assertThrows(NumberFormatException.class, () -> 
			{
				AES256.decrypt(original, bitStringKeyGreaterOne);
			});
			
			assertThrows(NumberFormatException.class, () -> 
			{
				AES256.decrypt(original, bitStringKeyWithChars);
			});
		}
		
		/*
		 * Note: Honestly not sure if all this NPE testing is even neccessary,
		 * if we just specify in the JavaDoc that the input may not be null.
		 * Oh well, now the tests are already written.
		 */
		
		@Test
		public void NPE_if_plaintext_is_ever_null() {
			String		nullInputStr = null;
			byte[]		nullInputArr = null;
			
			assertThrows(NullPointerException.class, () -> {
				SecretKey key = null;
				try{
					KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
					keyGen.init(256);
					key = keyGen.generateKey();
				}
				catch (Exception e) {
					System.out.println(KEYGEN_ERROR +e.toString());
					assertFalse(true, "Could not generate the key needed for this test.");
				}
				AES256.encrypt(nullInputArr, key);
			});
			
			assertThrows(NullPointerException.class, () -> {
				SecretKey key = null;
				try {
					KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
					keyGen.init(256);
					key = keyGen.generateKey();
				}
				catch (Exception e) {
					System.out.println(KEYGEN_ERROR +e.toString());
					assertFalse(true, "Could not generate the key needed for this test.");
				}
				AES256.encrypt(nullInputStr, key);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputStr, byteKey);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputStr, bitStringKey);
			});
			
		}
		
		@Test
		public void NPE_if_ciphertext_is_ever_null() {
			
			String		nullInputStr = null;
			byte[]		nullInputArr = null;
			
			assertThrows(NullPointerException.class, () -> {
				SecretKey key = null;
				try {
					KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
					keyGen.init(256);
					key = keyGen.generateKey();
				}
				catch (Exception e) {
					System.out.println(KEYGEN_ERROR +e.toString());
					assertFalse(true, "Could not generate the key needed for this test.");
				}
				AES256.decrypt(nullInputArr, key);
			});
			
			assertThrows(NullPointerException.class, () -> {
				SecretKey key = null;
				try {
					KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
					keyGen.init(256);
					key = keyGen.generateKey();
				}
				catch (Exception e) {
					System.out.println(KEYGEN_ERROR +e.toString());
					assertFalse(true, "Could not generate the key needed for this test.");
				}
				AES256.decrypt(nullInputStr, key);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputStr, byteKey);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputStr, bitStringKey);
			});
		}
		
		@Test
		public void NPE_if_key_is_ever_null() {
			SecretKey 	nullSk 	= null;
			byte[] 		nullByteSk 	= null;
			String 		nullStringSk = null;
			
			String		arbitraryInputStr = "12345";
			byte[]		arbitraryInputArr = arbitraryInputStr.getBytes();
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(arbitraryInputArr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(arbitraryInputStr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(arbitraryInputStr, nullByteSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(arbitraryInputStr, nullStringSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(arbitraryInputArr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(arbitraryInputStr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(arbitraryInputStr, nullByteSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(arbitraryInputStr, nullStringSk);
			});
		}
		
		@Test
		public void NPE_if_key_and_text_are_null() {
			SecretKey 	nullSk 			= null;
			byte[] 		nullByteSk 		= null;
			String 		nullStringSk 	= null;
			
			String		nullInputStr = null;
			byte[]		nullInputArr = null;
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputArr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputStr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputStr, nullByteSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.encrypt(nullInputStr, nullStringSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputArr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputStr, nullSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputStr, nullByteSk);
			});
			
			assertThrows(NullPointerException.class, () -> {
				AES256.decrypt(nullInputStr, nullStringSk);
			});
		}
	}
	
	@Nested
	class Crypto_Utility {
		
		@Nested
		class bitString256ToByteArray32 {
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
				assertThrows(IllegalArgumentException.class, () -> {CryptoUtility.bitString256ToByteArray32(bitStringKeyLong);});
			}
			
			@Test
			/*
			 * Testing the 256 char bit string to 32 bytes array method when using
			 * too short bit string
			 */
			public void testBitString256ToByteArray32TooShort() {
				assertThrows(IllegalArgumentException.class, () -> {CryptoUtility.bitString256ToByteArray32(bitStringKeyShort);});
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

		}
		
		@Nested
		class stringToSecretKeyAES256 {
			
			
			

			
			@Test
			/*
			 * Testing that correct Exception is thrown when using a too long byte array
			 */
			public void testStringToSecretKeyAES256ByteArraygTooLong() {
				assertThrows(IllegalArgumentException.class, () -> {CryptoUtility.byteArrayToSecretKeyAES256(byteKeyLong);});
			}
			
			@Test
			/*
			 * Testing that correct Exception is thrown when using a too short byte array
			 */
			public void testStringToSecretKeyAES256ByteArrayTooShort() {
				assertThrows(IllegalArgumentException.class, () -> {CryptoUtility.byteArrayToSecretKeyAES256(byteKeyShort);});
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
			 * Testing that correct Exception is thrown, when using a too short bit string
			 */
			public void testStringToSecretKeyAES256BitStringTooShort() {
				assertThrows(IllegalArgumentException.class,() -> {
					assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyShort));
				});
			}
			
			@Test
			/*
			 * Testing that correct Exception is thrown, when using a too long bit string
			 */
			public void testStringToSecretKeyAES256BitStringTooLong() {
				assertThrows(IllegalArgumentException.class,() -> {
					assertNull(CryptoUtility.stringToSecretKeyAES256(bitStringKeyLong));
				});
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
		
		@Nested
		class byteArrayToSecretKeyAES256 {
			@Test
			/*
			 * Testing that SecretKey object contains the correct key information.
			 */
			public void testByteArrayToSecretKeyAES256ValidByteArray() {
				SecretKey sk = CryptoUtility.byteArrayToSecretKeyAES256(byteKey);
				byte[] keyBytes = sk.getEncoded();
				for(int i = 0; i < 32; i++) {
					assertEquals(byteKey[i], keyBytes[i]);
				}
			}
			
			@Test
			/*
			 * Testing that correct Exception is thrown when null used as byte array
			 */
			public void testByteArrayToSecretKeyAES256Null() {
				assertThrows(NullPointerException.class, () -> {
					assertNull(CryptoUtility.byteArrayToSecretKeyAES256(null));
				});
			}
		}

	}
	
}
