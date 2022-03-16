import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import encryptionDecryption.AES256;
import encryptionDecryption.SymmetricCipher;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.CouldNotSendMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.PortIsInUseException;
import frame.QuantumnetworkControllcenter;
import messengerSystem.Authentication;
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
	static Authentication auth;

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
		/*
		 * Potential TODO:
		 * - Change Authentication from Interface to Abstract Class 
		 *   (https://www.tutorialspoint.com/when-to-use-an-abstract-class-and-when-to-use-an-interface-in-java)
		 * - Add settable field for the used CommunicationList
		 * --> this allows testing of the components without initializing the QNCC
		 * --> further decouples them
		 */
		QuantumnetworkControllcenter.communicationList = new SQLiteCommunicationList();
		CommunicationList commList = QuantumnetworkControllcenter.communicationList;
		
		// Delete previous entries if present
		commList.delete("Alice");
		commList.delete("Bob");
		
		// Generate key pair if none exists
		SHA256withRSAAuthentication.generateSignatureKeyPair("keys_for_testing_ms", true, false);
		
		// Public key used by both parties
		String pk = SHA256withRSAAuthentication.readKeyStringFromFile("keys_for_testing_ms.pub");
		
		commList.insert("Alice", "127.0.0.1", 60050, pk);
		commList.insert("Bob", "127.0.0.2", 60040, pk); 
		
		auth = new SHA256withRSAAuthentication();
	}
	
	@AfterEach
	public void resetLogs() {
		AliceCM.getConnectionEndpoint("Bob").getPackageLog().clear();
		BobCM.getConnectionEndpoint("Alice").getPackageLog().clear();
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
		ArrayList<NetworkPackage> textMessagesReceivedByBob 	= connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		ArrayList<NetworkPackage> textMessagesReceivedByAlice 	= connectionToBob.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		assertEquals(1, textMessagesReceivedByBob.size());			// Bob received a message
		assertEquals(0, textMessagesReceivedByAlice.size());		// Alice did not receive a message
		assertEquals(aliceMessageToBob, MessageSystem.byteArrayToString(textMessagesReceivedByBob.get(0).getContent())); // Bob received exactly the message sent
		
		// Bob sends Message to Alice
		MessageSystem.conMan = BobCM;
		String bobMessageToAlice = "Hallo Alice.";
		MessageSystem.sendTextMessage("Alice", bobMessageToAlice, false, false);
		waitBriefly();
		// Check states of both of their package stacks
		textMessagesReceivedByBob 		= connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		textMessagesReceivedByAlice 	= connectionToBob.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		assertEquals(1, textMessagesReceivedByBob.size());			// Bob still has exactly one message received
		assertEquals(1, textMessagesReceivedByAlice.size());		// Alice received a message now
		assertEquals(bobMessageToAlice, MessageSystem.byteArrayToString(textMessagesReceivedByAlice.get(0).getContent())); // Alice received exactly the message sent
		
	}
	
	@Test
	public void test_authenticated_messaging() throws CouldNotSendMessageException, EndpointIsNotConnectedException {
		
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
		ArrayList<NetworkPackage> bobsTextMessages = connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE);
		assertEquals(1, bobsTextMessages.size(), "Bob should have received exactly one text message.");
		NetworkPackage msgReceivedByBob = bobsTextMessages.get(0);
		assertArrayEquals(aliceMessageToBob, msgReceivedByBob.getContent());
		
		// Attempt to pass Bob the altered message
		NetworkPackageHandler.handlePackage(connectionToAlice, alteredMessagePackage);
		// No new message should have arrived
		assertEquals(1, bobsTextMessages.size(), "Bob should have received exactly one text message.");
	}
	
	public void test_encrypted_text_message() {
		assertFalse(true, "Not implemented yet.");
	}
	
	public void test_encrypted_file_transfer() {
		assertFalse(true, "Not implemented yet.");
	}
	
	@Test
	public void other_tests() {
		assertFalse(true, "Not implemented yet.");
	}
	
	private static void waitBriefly() {
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}
