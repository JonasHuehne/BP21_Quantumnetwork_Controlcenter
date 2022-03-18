import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Random;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import encryptionDecryption.SymmetricCipher;
import exceptions.CouldNotDecryptMessageException;
import exceptions.CouldNotEncryptMessageException;
import exceptions.CouldNotGetKeyException;
import exceptions.ExternalApiNotInitializedException;
import exceptions.PortIsInUseException;
import externalAPI.ExternalAPI;
import frame.Configuration;
import keyStore.KeyStoreDbManager;

/**
 * Tests for the External API.
 * @author Lukas Dentler, Sasha Petri
 */
public class ExternalAPITests {

	private Path currentWorkingDir = Path.of(Configuration.getBaseDirPath());
	private Path externalPath = currentWorkingDir.resolve("externalAPI");
	
	private static byte[] key = new byte[512];
	
	@BeforeAll
	public static void init() throws IOException, PortIsInUseException, SQLException {
		ExternalAPI.initialize("Alice", "127.0.0.1", 60000);
		KeyStoreDbManager.createNewKeyStoreAndTable();
		Random r = new Random();
		r.nextBytes(key);
	}
	
	@BeforeEach
	public void reset() throws SQLException {
		KeyStoreDbManager.deleteEntryIfExists("ExtApiTester");
		KeyStoreDbManager.insertToKeyStore("ExtApiTester", key, "", "", false, true);
	}
	
	@Test
	public void test_not_initialized_exception() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// assert API was initialized before
		assertTrue(ExternalAPI.isInitialized());
		// Use reflection to set initialized to false
		Field init = ExternalAPI.class.getDeclaredField("initialized");
		init.setAccessible(true);
		init.set(null, false);
		// assert reflection set field correctly
		assertFalse(ExternalAPI.isInitialized());
		
		// assert Exception thrown when it should
		assertThrows(ExternalApiNotInitializedException.class, () -> ExternalAPI.connectTo("127.0.0.1", 1000, "Bob", ""));
		assertThrows(ExternalApiNotInitializedException.class, () -> ExternalAPI.sendEncryptedTxtFile("Bob", new File("Blabla.txt")));
				
		// Set it to true again
		init.set(null, true);
		assertTrue(ExternalAPI.isInitialized());
	}
	
	@Test
	public void test_export_key() throws CouldNotGetKeyException {
		SymmetricCipher cipher = ExternalAPI.getCipher();
		int keyLengthBytes = cipher.getKeyLength() / 8;
		byte[] firstKeyBytes = ExternalAPI.exportKeyByteArray("ExtApiTester", keyLengthBytes, 0);
		byte[] someOtherKeyBytes = ExternalAPI.exportKeyByteArray("ExtApiTester", keyLengthBytes, 10);
		
		byte[] expectedFirstBytes = new byte[keyLengthBytes];
		byte[] expectedOtherBytes = new byte[keyLengthBytes];
		System.arraycopy(key, 0, expectedFirstBytes, 0, keyLengthBytes);
		System.arraycopy(key, 10, expectedOtherBytes, 0, keyLengthBytes);
		
		assertArrayEquals(expectedFirstBytes, firstKeyBytes);
		assertArrayEquals(expectedOtherBytes, someOtherKeyBytes);
		
		SecretKey expectedSkFirstBytes = cipher.byteArrayToSecretKey(expectedFirstBytes);
		SecretKey expectedSkNextBytes = cipher.byteArrayToSecretKey(expectedOtherBytes);
		
		SecretKey receivedFirstSk = ExternalAPI.exportKeySecretKey("ExtApiTester", 0);
		SecretKey receivedNextSk = ExternalAPI.exportKeySecretKey("ExtApiTester", 10);
		
		assertEquals(expectedSkFirstBytes, receivedFirstSk);
		assertEquals(expectedSkNextBytes, receivedNextSk);
	}
	
	
	@Test
	public void encrypt_decrypt_test() throws IOException, CouldNotGetKeyException, CouldNotEncryptMessageException, CouldNotDecryptMessageException, InvalidKeyException, IllegalBlockSizeException {
		Path testFile = Path.of(System.getProperty("user.dir"), "ExampleContent", "FilesForTransferTests", "TestImage.png");
		Path encryptedFileLocation = externalPath.resolve("encr_index_0_TestImage.png");
		Path decryptedFileLocation = externalPath.resolve("decr_encr_index_0_TestImage.png");
		assertTrue(Files.exists(testFile), "Test file does not exist.");
		Files.deleteIfExists(encryptedFileLocation);
		Files.deleteIfExists(decryptedFileLocation);
		// enc dec files should no longer exist
		assertFalse(Files.exists(encryptedFileLocation));
		assertFalse(Files.exists(decryptedFileLocation));
		
		// Encrypt file
		ExternalAPI.encryptFile("ExtApiTester", testFile.toFile());
		// Should have created file 
		assertTrue(Files.exists(encryptedFileLocation));
		
		// Decrypt File
		ExternalAPI.decryptFile("ExtApiTester", 0, encryptedFileLocation.toFile());
		
		byte[] originalBytes = Files.readAllBytes(testFile);
		byte[] encrFileBytes = Files.readAllBytes(encryptedFileLocation);
		byte[] decrFileBytes = Files.readAllBytes(decryptedFileLocation);
		
		// decrypted == original
		assertArrayEquals(originalBytes, decrFileBytes);
		
		// Encryption actually uses the cipher, also uses the correct part of the key
		SymmetricCipher cipher = ExternalAPI.getCipher();
		byte[] expectedKeyUsed = new byte[cipher.getKeyLength() / 8];
		System.arraycopy(key, 0, expectedKeyUsed, 0, cipher.getKeyLength() / 8);
		byte[] expectedEncBytes = cipher.encrypt(originalBytes, expectedKeyUsed);
		assertArrayEquals(expectedEncBytes, encrFileBytes);
	}
	
	
	@Test
	public void testMessaging() {
		
		// TODO test the methods for sending and receiving (encrypted) text messages
		
	}
}