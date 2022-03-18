

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

/**
 * Small Test for the {@linkplain FileCrypter} class
 * @author Sasha Petri
 */
public class FileCrypterTests {

	@Test
	public void fileCrypterWorksWithAES256() throws InvalidKeyException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException {
		SymmetricCipher cipher = new AES256();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey key = keyGen.generateKey();
		File cat = new File("ExampleContent/FilesForTransferTests/TestImage.png");
		Path outPath = Paths.get("ExampleContent/FilesForTransferTests/fc_TestImage_encrypted.png");
		Path outPathDec = Paths.get("ExampleContent/FilesForTransferTests/fc_TestImage_decrypted.png");

		// delete enc / dec file if they exist
		Files.deleteIfExists(outPath);
		Files.deleteIfExists(outPathDec);

		// encrypt the test image
		FileCrypter.encryptAndSave(cat, cipher, key, outPath);
		// decrypt it again
		FileCrypter.decryptAndSave(outPath.toFile(), cipher, key, outPathDec);

		// check they are the same
		byte[] baseBytes = Files.readAllBytes(cat.toPath());
		byte[] decBytes  = Files.readAllBytes(outPathDec);

		assertArrayEquals(baseBytes, decBytes);

		// clean up
		Files.deleteIfExists(outPath);
		Files.deleteIfExists(outPathDec);
	}

}