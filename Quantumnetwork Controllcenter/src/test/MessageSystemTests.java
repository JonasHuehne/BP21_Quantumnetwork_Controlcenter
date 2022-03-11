import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import encryptionDecryption.AES256;
import encryptionDecryption.SymmetricCipher;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.CouldNotSendMessageException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.PortIsInUseException;
import frame.QuantumnetworkControllcenter;
import messengerSystem.Authentication;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;

/**
 * Tests for the sending and receiving of messages via the {@link MessageSystem} class. <br>
 * Just like the MessageSystem requires a functioning network, these tests do so as well.
 * @author Sasha Petri
 */
public class MessageSystemTests {
	
	static ConnectionManager AliceCM, BobCM;

	@BeforeAll
	public static void initialize() throws IOException, PortIsInUseException, IpAndPortAlreadyInUseException {
		Authentication auth = new SHA256withRSAAuthentication();
		SymmetricCipher cipher = new AES256();
		
		// Simulate two Participants in the System
		AliceCM = new ConnectionManager("127.0.0.1", 60050, "Alice");
		BobCM = new ConnectionManager("127.0.0.1", 60040, "Bob");
		
		// Because MessageSystem is static, we will have to swap the CM used by it occasionally...
		// Regardless, both participants use the same auth and encryption algorithm
		MessageSystem.setAuthenticationAlgorithm(auth);
		MessageSystem.setEncryption(cipher);
		
		// We have two participants: Alice and Bob
		try {
			// Alice connects to Bob
			AliceCM.createNewConnectionEndpoint("Bob", "127.0.0.1", 60040);
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
	}
	
	@Test
	public void test_basic_messaging() throws CouldNotSendMessageException {
		ConnectionEndpoint connectionToAlice = BobCM.getConnectionEndpoint("Alice"); 
		ConnectionEndpoint connectionToBob = AliceCM.getConnectionEndpoint("Bob");
		
		// Alice sends Message to Bob
		MessageSystem.conMan = AliceCM;
		MessageSystem.sendTextMessage("Bob", "Hola Bob", false, false);
		waitBriefly();
		int textMessagesReceivedByBob 	= connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE).size();
		int textMessagesReceivedByAlice = connectionToBob.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE).size();
		assertEquals(1, textMessagesReceivedByBob);
		assertEquals(0, textMessagesReceivedByAlice);
		
		// Bob sends Message to Alice
		MessageSystem.conMan = BobCM;
		MessageSystem.sendTextMessage("Alice", "Hallo Alice.", false, false);
		waitBriefly();
		textMessagesReceivedByBob 	= connectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE).size();
		textMessagesReceivedByAlice = connectionToBob.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE).size();
		assertEquals(1, textMessagesReceivedByBob);
		assertEquals(1, textMessagesReceivedByAlice);
		
	}
	
	public void test_authenticated_messaging() {
		assertFalse(true, "Not implemented yet.");
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
