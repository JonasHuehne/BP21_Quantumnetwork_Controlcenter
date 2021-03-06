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
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.IllegalBlockSizeException;

import messengerSystem.SignatureAuthentication;
import messengerSystem.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import encryptionDecryption.AES256;
import encryptionDecryption.SymmetricCipher;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.CouldNotDecryptMessageException;
import exceptions.CouldNotSendMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import exceptions.PortIsInUseException;
import exceptions.VerificationFailedException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import keyStore.KeyStoreDbManager;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.MessageArgs;
import networkConnection.NetworkPackage;
import networkConnection.NetworkPackageHandler;
import networkConnection.TransmissionTypeEnum;

/**
 * Tests for the sending and receiving of messages via the {@link MessageSystem} class. <br>
 * Just like the MessageSystem requires a functioning network, these tests do so as well.
 * @author Sasha Petri
 */
public class MessageSystemTests {
	
	static ConnectionManager AliceCM, BobCM;
	static SignatureAuthentication auth;

	@BeforeAll
	public static void initialize() throws IOException, PortIsInUseException, IpAndPortAlreadyInUseException {
		auth = new SHA256withRSAAuthentication();
		SymmetricCipher cipher = new AES256();
		
		// Simulate two Participants in the System
		AliceCM = new ConnectionManager("127.0.0.1", 60050, "Alice", null);
		BobCM = new ConnectionManager("127.0.0.1", 60040, "Bob", null);
		
		// Because MessageSystem is static, we will have to swap the CM used by it occasionally...
		// Regardless, both participants use the same auth and encryption algorithm
		MessageSystem.setAuthenticationAlgorithm(auth);
		MessageSystem.setEncryption(cipher);
		
		// We have two participants: Alice and Bob
		try {
			// Alice connects to Bob
			AliceCM.createNewConnectionEndpoint("Bob", "127.0.0.1", 60040, null);
			waitBriefly();
			assertEquals(ConnectionState.CONNECTED, AliceCM.getConnectionEndpoint("Bob").reportState());
			// Bob should also be connected to Alice
			assertEquals(ConnectionState.CONNECTED, BobCM.getConnectionEndpoint("Alice").reportState());
		} catch (ConnectionAlreadyExistsException e) {
			// not thrown
		} catch (IpAndPortAlreadyInUseException e) {
			throw e;
		} 
		// Both use the same public key, because we only have one private key for local signing
		// (limitation of local testing, could be solved via mock classes, but no time)
		
		// Initialize only the relevant part of the QNCC

		QuantumnetworkControllcenter.communicationList = new SQLiteCommunicationList();
		CommunicationList commList = QuantumnetworkControllcenter.communicationList;
		
		// Delete previous entries if present
		commList.delete("Alice");
		commList.delete("Bob");
		
		// Generate key pair if none exists
		auth.generateSignatureKeyPair("signature", true, false, false);
		
		// Public key used by both parties
		String pk = Utils.readKeyStringFromFile("signature.pub");
		
		commList.insert("Alice", "127.0.0.1", 60050, pk);
		commList.insert("Bob", "127.0.0.2", 60040, pk); 
		
		auth = new SHA256withRSAAuthentication();
		
	}
	
	@BeforeEach
	public void reset() throws SQLException {
		AliceCM.getConnectionEndpoint("Bob").getPackageLog().clear();
		AliceCM.getConnectionEndpoint("Bob").getChatLog().clear();
		BobCM.getConnectionEndpoint("Alice").getPackageLog().clear();
		BobCM.getConnectionEndpoint("Alice").getChatLog().clear();
		
		KeyStoreDbManager.deleteEntryIfExists("Alice");
		KeyStoreDbManager.deleteEntryIfExists("Bob");
	}
	
	@Test
	public void test_basic_messaging() throws CouldNotSendMessageException {
		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice"); 
		ConnectionEndpoint connectionToBob = AliceCM.getConnectionEndpoint("Bob");
		
		// Alice sends Message to Bob
		MessageSystem.conMan = AliceCM;
		String aliceMessageToBob = "Hola Bob";
		MessageSystem.sendTextMessage("Bob", aliceMessageToBob, false, false);
		waitBriefly();
		// Check states of both of their package stacks
		ArrayList<SimpleEntry<String, String>> textMessagesReceivedByBob 	= connectionToAlice.getChatLog();
		ArrayList<SimpleEntry<String, String>> textMessagesReceivedByAlice 	= connectionToBob.getChatLog();
		assertEquals(1, textMessagesReceivedByBob.size());								// Bob received a message
		assertEquals(0, textMessagesReceivedByAlice.size());							// Alice did not receive a message
		assertEquals(aliceMessageToBob, textMessagesReceivedByBob.get(0).getValue()); 	// Bob received exactly the message sent
		
		// Bob sends Message to Alice
		MessageSystem.conMan = BobCM;
		String bobMessageToAlice = "Hallo Alice.";
		MessageSystem.sendTextMessage("Alice", bobMessageToAlice, false, false);
		waitBriefly();
		// Check states of both of their package stacks
		textMessagesReceivedByBob 	= connectionToAlice.getChatLog();
		textMessagesReceivedByAlice 	= connectionToBob.getChatLog();
		assertEquals(1, textMessagesReceivedByBob.size());								// Bob still has exactly one message received
		assertEquals(1, textMessagesReceivedByAlice.size());							// Alice received a message now
		assertEquals(bobMessageToAlice, textMessagesReceivedByAlice.get(0).getValue()); // Alice received exactly the message sent
		
	}
	
	@Test
	public void test_authenticated_messaging() throws CouldNotSendMessageException, EndpointIsNotConnectedException, CouldNotDecryptMessageException {
		
		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice"); 
		ConnectionEndpoint connectionToBob = AliceCM.getConnectionEndpoint("Bob");
		
		/*
		 * Preparation
		 */
		
		// Message that Alice will send to Bob
		String messageString = "This is a very important message that should not be altered, so we are signing and verifying it.";
		byte[] aliceMessageToBob = MessageSystem.stringToByteArray(messageString);
		byte[] alteredMessageToBob = MessageSystem.stringToByteArray("This is an altered message that should not be verified.");
		
		// Construct a package indentical to what Alice should send to Bob
		NetworkPackage messageThatShouldBeSent = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, new MessageArgs(), aliceMessageToBob, false);
		messageThatShouldBeSent.sign(auth);
		// Signature should be valid
		assertTrue(messageThatShouldBeSent.verify(auth, "Alice"));
		
		// Construct a package that's a mockup of a message altered after Alice sent it, but before Bob received it
		NetworkPackage alteredMessagePackage = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, new MessageArgs(), aliceMessageToBob, false);
		alteredMessagePackage.sign(auth);
		
		// Alter the contents of the message (using reflection)
		try {
			Field messageContents = alteredMessagePackage.getClass().getDeclaredField("content");
			messageContents.setAccessible(true);
			messageContents.set(alteredMessagePackage, alteredMessageToBob);
			// assert that the alteration was successful
			assertArrayEquals(alteredMessagePackage.getContent(), alteredMessageToBob, "Failed to alter contents of altered message.");
			// signature should now be invalid
			assertFalse(alteredMessagePackage.verify(auth, "Alice"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false, "Something went wrong, most likely with reflection.");
		}
		
		/*
		 * Communication
		 */
		
		// Alice sends auth message to Bob
		MessageSystem.conMan = AliceCM;
		MessageSystem.sendTextMessage("Bob", messageString, true, false);
		waitBriefly();
		
		// Check that message arrived and was added to Bob's received messages
		ArrayList<SimpleEntry<String, String>> bobsTextMessages = connectionToAlice.getChatLog();
		assertEquals(1, bobsTextMessages.size(), "Bob should have received exactly one text message.");
		assertEquals(messageString, bobsTextMessages.get(0).getValue());
		
		// Attempt to pass Bob the altered message
		assertThrows(VerificationFailedException.class, () -> NetworkPackageHandler.handlePackage(connectionToAlice, alteredMessagePackage));
		
		// No new message should have arrived
		assertEquals(1, bobsTextMessages.size(), "Bob should have received exactly one text message.");
	}
	
	@Test
	public void test_encrypted_text_message() throws SQLException, CouldNotSendMessageException, InvalidKeyException, 
	IllegalBlockSizeException, NoKeyWithThatIDException, EndpointIsNotConnectedException, CouldNotDecryptMessageException, VerificationFailedException {
		
		// In this test, Alice will send an encrypted text message to Bob
		
		/*
		 * Preparation
		 */

		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice");  	// Bob's Connection to Alice
		ConnectionEndpoint connectionToBob = AliceCM.getConnectionEndpoint("Bob");		// Alice Connection to Bob

		// Alice and Bob have generated a mutual key (Alice is the one who had the initiative)
		byte[] randomKey = new byte[1024];
		Random r = new Random();
		r.nextBytes(randomKey);
		
		KeyStoreDbManager.insertToKeyStore("Alice", randomKey, "", "", false, false); // Bob's entry for his connection to Alice
		connectionToAlice.setKeyStoreID("Alice");
		
		KeyStoreDbManager.insertToKeyStore("Bob", randomKey, "", "", false, true); // Alice entry for her connection Bob
		connectionToBob.setKeyStoreID("Bob");
		
		String secretString = "This is a secret message";
		
		// Construct the package that Alice should send to Bob (message encrypted with the first KEY_LENGTH / 8 bytes of the mutual key)
		byte[] initialKeyUsed = new byte[MessageSystem.getCipher().getKeyLength() / 8];
		System.arraycopy(randomKey, 0, initialKeyUsed, 0, initialKeyUsed.length);
		byte[] encryptedBytes = MessageSystem.getCipher().encrypt(MessageSystem.stringToByteArray(secretString), initialKeyUsed);
		MessageArgs args = new MessageArgs(0);
		NetworkPackage encryptedPackage = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, args, encryptedBytes, false);
		encryptedPackage.sign(auth); // all encrypted packages are signed
		
		// Also construct a corrupted package that has been altered along the way, invalidating the signature
		NetworkPackage alteredPackage = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, args, encryptedBytes, false);
		alteredPackage.sign(auth);
		
		// Alter the contents of the message (using reflection)
		try {
			byte[] alteredContents = new byte[encryptedBytes.length];
			System.arraycopy(encryptedBytes, 0, alteredContents, 0, alteredContents.length);
			alteredContents[0] = (byte) (alteredContents[0] + 1);
			
			Field messageContents = alteredPackage.getClass().getDeclaredField("content");
			messageContents.setAccessible(true);
			messageContents.set(alteredPackage, alteredContents);
			// assert that the alteration was successful
			assertArrayEquals(alteredPackage.getContent(), alteredContents, "Failed to alter contents of altered message.");
			// signature should now be invalid
			assertFalse(alteredPackage.verify(auth, "Alice"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false, "Something went wrong, most likely with reflection.");
		}
		
		/*
		 * Communication
		 */
		MessageSystem.conMan = AliceCM;
		// Alice will send a message to Bob
		MessageSystem.sendEncryptedTextMessage("Bob", "This is a secret message", false);
		waitBriefly();
		
		
		// Encrypted Message is in package log
		ArrayList<NetworkPackage> bobsPackageLog = connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		assertEquals(1, bobsPackageLog.size());
		assertArrayEquals(encryptedBytes, bobsPackageLog.get(0).getContent());
		
		// Decrypted Message is in chat log for both Bob and Alice
		ArrayList<SimpleEntry<String, String>> bobsChatLog = connectionToAlice.getChatLog();
		assertEquals(secretString, bobsChatLog.get(0).getValue());
		ArrayList<SimpleEntry<String, String>> aliceChatLog = connectionToAlice.getChatLog();
		assertEquals(secretString, aliceChatLog.get(0).getValue());
		
		// Altered message fails verification
		assertThrows(VerificationFailedException.class, () -> NetworkPackageHandler.handlePackage(connectionToAlice, alteredPackage));
		
		// Altered message does not result in anything more being chat-logged by Bob
		bobsChatLog = connectionToAlice.getChatLog();
		aliceChatLog = connectionToAlice.getChatLog();
		assertEquals(1, bobsChatLog.size());
		assertEquals(1, aliceChatLog.size());
		
		// Key Indexes of both parties are adjusted accordingly
		int expectedIndex = MessageSystem.getCipher().getKeyLength() / 8;
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToAlice.getKeyStoreID()));
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToBob.getKeyStoreID()));
		
		
		/// EDGE CASE 1: A's Index is > B's index ///
		KeyStoreDbManager.incrementIndex(connectionToBob.getKeyStoreID(), 10);
		MessageSystem.sendEncryptedTextMessage("Bob", secretString + "v2", false);
		waitBriefly();
		// Bob should have received the message
		bobsChatLog = connectionToAlice.getChatLog();
		assertEquals(secretString + "v2", bobsChatLog.get(bobsChatLog.size() - 1).getValue());
		// Both should have the same index (KEY_LENGTH_BYTES * 2 + 10) now
		expectedIndex = (MessageSystem.getCipher().getKeyLength() / 8) * 2 + 10;
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToAlice.getKeyStoreID()));
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToBob.getKeyStoreID()));
		
		
		
		/// EDGE CASE 2: A's Index is < B's index ///
		int bobChatSizePreSend = bobsChatLog.size();
		KeyStoreDbManager.incrementIndex(connectionToAlice.getKeyStoreID(), 10);
		MessageSystem.sendEncryptedTextMessage("Bob", secretString + "v3", false);
		waitBriefly();
		// Bob's chat log should not have increased in size
		bobsChatLog = connectionToAlice.getChatLog();
		assertEquals(bobChatSizePreSend, bobsChatLog.size(), "Bob should not have received a new message from Alice, "
				+ "however, Bob did. The message is " + bobsChatLog.get(bobsChatLog.size() - 1));
		// Alice should have received a rejection (logged)
		assertEquals(1, connectionToBob.getLoggedPackagesOfType(TransmissionTypeEnum.KEY_USE_REJECT).size());
		// Both should now have Bob's index previous to Alice sending the message
		expectedIndex = (MessageSystem.getCipher().getKeyLength() / 8) * 2 + 20;
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToAlice.getKeyStoreID()));
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToBob.getKeyStoreID()));

		
		// Assert that Alice Index doesn't change if she receives a rejection with an index < her own
		NetworkPackage artificialRejection = new NetworkPackage(TransmissionTypeEnum.KEY_USE_REJECT, new MessageArgs(expectedIndex - 12), false);
		NetworkPackageHandler.handlePackage(connectionToBob, artificialRejection);
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToAlice.getKeyStoreID()));
		assertEquals(expectedIndex, KeyStoreDbManager.getIndex(connectionToBob.getKeyStoreID()));
		
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "TestImage.png", "TestODT.odt", "TestPDF.pdf", "TestTXT.txt", "TestZip.zip" })
	public void test_file_transfer(String fileName) throws CouldNotSendMessageException, IOException {
		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice");  	// Bob's Connection to Alice

		MessageSystem.conMan = AliceCM;
		File f = Paths.get("ExampleContent", "FilesForTransferTests", fileName).toFile();
		assertTrue(f.exists(), "The test file for transfer does not exist.");
		// Delete the file that would result from transfer
		Path outPath = Paths.get(Configuration.getBaseDirPath(), "ReceivedFiles", connectionToAlice.getRemoteName(), fileName);
		Files.deleteIfExists(outPath);
		// Send the file, unencrypted and unverified
		MessageSystem.sendFile("Bob", f, false, false);
		waitBriefly();
		// There should now be a file at the outpath
		assertTrue(Files.exists(outPath));
		// it should be identical to the file that was sent
		byte[] originalFileBytes = Files.readAllBytes(f.toPath());
		byte[] outPutFileBytes = Files.readAllBytes(outPath);
		assertArrayEquals(originalFileBytes, outPutFileBytes);
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "TestImage.png", "TestODT.odt", "TestPDF.pdf", "TestTXT.txt", "TestZip.zip" })
	public void test_encrypted_file_transfer(String fileName) throws CouldNotSendMessageException, IOException, SQLException, NoKeyWithThatIDException, NotEnoughKeyLeftException, InvalidKeyException, IllegalBlockSizeException {
		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice");  	// Bob's Connection to Alice
		ConnectionEndpoint connectionToBob = AliceCM.getConnectionEndpoint("Bob");		// Alice Connection to Bob

		/*
		 * Preparation
		 */
		
		MessageSystem.conMan = AliceCM;
		
		// Alice and Bob have generated a mutual key (Alice is the one who had the initiative)
		byte[] randomKey = new byte[1024];
		Random r = new Random();
		r.nextBytes(randomKey);
				
		KeyStoreDbManager.insertToKeyStore("Alice", randomKey, "", "", false, false); // Bob's entry for his connection to Alice
		connectionToAlice.setKeyStoreID("Alice");
				
		KeyStoreDbManager.insertToKeyStore("Bob", randomKey, "", "", false, true); // Alice entry for her connection Bob
		connectionToBob.setKeyStoreID("Bob");
		
		
		File f = Paths.get("ExampleContent", "FilesForTransferTests", fileName).toFile();
		// Check that file to transfer exists
		assertTrue(f.exists(), "The test file for transfer does not exist.");
		// Delete the file that would result from transfer, if it exists from previous tests
		Path outPath = Paths.get(Configuration.getBaseDirPath(), "ReceivedFiles", connectionToAlice.getRemoteName(), fileName);
		Files.deleteIfExists(outPath);
		// Also delete the decrypted version
		Path outPathDec = Paths.get(Configuration.getBaseDirPath(), "ReceivedFiles", connectionToAlice.getRemoteName(), "decrypted_" + fileName);
		Files.deleteIfExists(outPathDec);
		// Send the file, encrypted
		MessageSystem.sendEncryptedFile("Bob", f, false);
		waitBriefly();
		// There should now be two files, one decrypted and one not
		assertTrue(Files.exists(outPath));
		assertTrue(Files.exists(outPathDec));
		
		// Decrypted file is identical to original
		byte[] originalBytes		 = Files.readAllBytes(f.toPath());
		byte[] decryptedBytesRead	 = Files.readAllBytes(outPathDec);
		
		assertArrayEquals(originalBytes, decryptedBytesRead);
		
		// Encryption was correct
		byte[] encryptedBytesRead	= Files.readAllBytes(outPath);
		byte[] key = KeyStoreDbManager.getKeyBytesAtIndexN(connectionToBob.getKeyStoreID(), MessageSystem.getCipher().getKeyLength() / 8, 0);
		byte[] artificiallyEncryptedBytes = MessageSystem.getCipher().encrypt(originalBytes, key);
		
		assertArrayEquals(encryptedBytesRead, artificiallyEncryptedBytes);
		
	}
	
	private static void waitBriefly() {
		try {
			TimeUnit.MILLISECONDS.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
