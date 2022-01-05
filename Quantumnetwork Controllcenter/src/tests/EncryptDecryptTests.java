package tests;

import encryptionDecryption.AES256;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;



public class EncryptDecryptTests {
	String original = "This is a Test!\"§$%&/()=?öäü to be fair a very long test text to see if it really works. So i am just going to smash my head on the keyboard a bit more: ajodaohglaenkohadoibhlaknowehojobhaskdjnfoaishcvon'#*_-:.,,;<>|adasogphopaidfhgvpoiiruhgaowenklödaioühoigüoh";
	String bitStringKey = "0101010110101011101010111011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
	
	
	@Test
	/*
	 * Testing encryption and decryption with a bitString as Key
	 */
	public void testEncryptionDecryptionBitStringKey(){
		String encrypted = AES256.encrypt(original, bitStringKey);
		String decrypted = AES256.decrypt(encrypted, bitStringKey);
			
		assertFalse(original.equals(encrypted));
		assertFalse(encrypted.equals(decrypted));
		assertTrue(decrypted.equals(original));
	}
	
	@Test
	/*
	 * Testing encryption and decryption with a generated Key
	 */
	public void testEncryptionDecryptionGeneratedKey() {
		String strKey = "";
		try{
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			SecretKey key = keyGen.generateKey();
			strKey = Base64.getEncoder().encodeToString(key.getEncoded());
		}
		catch (Exception e) {
			System.out.println("An ERROR occured during the keygeneration:" +e.toString());
		}
			
		String encrypted = AES256.encrypt(original, strKey);
		String decrypted = AES256.decrypt(encrypted, strKey);
		
		assertFalse(original.equals(encrypted));
		assertFalse(encrypted.equals(decrypted));
		assertTrue(decrypted.equals(original));
	}
	
	/*
	 * Class put into comments since method is private
	 * ran the test once with class set public and everything worked
	 * 
	 * @Test
	 *
	 * Testing the 256 char bitString to 32 bytes array method 
	 * with extreme cases:
	 * max value
	 * min value
	 * zero
	 * and random values between 
	 *
	public void testBitString256ToByteArray32() {
		String bitString = "0000000011111111100000001011101010101010101010101110011110000011101010111010111011100011000011110111101111101011010101011010101010111000011101010111101011110110111110001111100001111100011111010101110101110101110101110111101111000001111111010101010111101111";
		byte[] byteArray = AES256.bitString256ToByteArray32(bitString);
		assertTrue(byteArray[0] == -128);
		assertTrue(byteArray[1] == 127);
		assertTrue(byteArray[2] == 0);
	}
	*/
}
