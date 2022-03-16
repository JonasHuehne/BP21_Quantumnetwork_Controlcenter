

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import encryptionDecryption.AES256;
import encryptionDecryption.FileCrypter;
import encryptionDecryption.SymmetricCipher;

public class FileCrypterTests {

	@Test
	public void fileCrypterWorksWithAES256() throws InvalidKeyException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException {
		SymmetricCipher cipher = new AES256();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey key = keyGen.generateKey();
		File cat = new File("ExampleContent/good_cat.png");
		Path outPath = Paths.get("ExampleContent/good_cat_encrypted");
		Path outPathDec = Paths.get("ExampleContent/good_cat_decrypted.png");

		// encrypt the test image
		FileCrypter.encryptAndSave(cat, cipher, key, outPath);
		// decrypt it again
		FileCrypter.decryptAndSave(outPath.toFile(), cipher, key, outPathDec);
		
		// check they are the same
		byte[] baseBytes = Files.readAllBytes(cat.toPath());
		byte[] decBytes  = Files.readAllBytes(outPathDec);
		
		assertArrayEquals(baseBytes, decBytes);
	}
	
}
