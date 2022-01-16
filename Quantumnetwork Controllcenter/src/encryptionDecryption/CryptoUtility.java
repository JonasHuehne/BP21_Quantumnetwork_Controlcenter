package encryptionDecryption;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for conversion of different inputs into byte arrays and Secret Keys
 * 
 * @author Lukas Dentler
 */
public class CryptoUtility {
	//constants
	private final static int BIT_STRING_LENGTH_AES256 = 256;
	private final static String ALGORITHM_AES = "AES";
	
	/**
	 * Used to convert a bit string containing 256 bits into an byteArray containing 32 bytes
	 * 
	 * @param str A string consisting of 256 characters, each one being either 0 or 1
	 * @return A byteArray containing 32 bytes corresponding to the 256 bits of str
	 * 		   returns null if str is not fulfilling the contracted specification.
	 */
	public static byte[] bitString256ToByteArray32(String str) throws NullPointerException, NumberFormatException {
		
		//initialize local variables
		byte[] bytes = new byte[32];
		int j = 0;
		
		//checking that str has the right length
		if (str.length() != BIT_STRING_LENGTH_AES256) {
			System.out.println("Please only use 256 char long bit strings");
			return null;
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
	 */
	public static SecretKey stringToSecretKeyAES256(String str) throws NullPointerException, NumberFormatException {

		//checking that str has the right length
		if (str.length() != BIT_STRING_LENGTH_AES256) {
			System.err.println("An invalid key was used. Please use a key of length 256");
			return null;
		}
		
		//converting String to byte array
		byte [] byteKey = bitString256ToByteArray32(str);
		
		//wrapping key information in SecretKey class for AES 
		return new SecretKeySpec(byteKey, 0, byteKey.length, ALGORITHM_AES);
	}
	
}
