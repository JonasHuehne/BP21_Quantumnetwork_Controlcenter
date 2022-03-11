import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.QuantumnetworkControllcenter;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import ui.Command;
import ui.CommandHandler;
import ui.ConnectionCommandHandler;

/**
 * This class is used to perform unit tests for the {@link CommandHandler} class,
 * specifically to see if it can deal with unexpected inputs,
 * and if the application behaves as expected on regular inputs
 * (e.g. it is possible to send a message between two nodes with the correct command).
 * 
 * Please keep in mind that errors of the tests performed here may not necessarily mean that 
 * the {@link CommandHandler} class is broken, it could be that the methods the {@link CommandHandler} is calling are broken.
 * 
 * @author Sasha Petri
 * @deprecated Due to time concerns, the focus of developement has shifted to the GUI. 
 * Support for the Console UI may be picked up again later, but at the moment there is no guarantee for it to be up to date or functional.
*/
@Deprecated
class CommandHandlerTest {
	
	/*
	 * Make a string for every command we are testing here, containing
	 * that command's name. This is to avoid the problem of having to
	 * manually change many Strings like "contacts add ..." if the name
	 * of a command were to ever change.
	 * We could use Command.COMMAND_NAME.getCommandName() every time,
	 * but this is fewer function calls and less text to read.
	 */
	private static final String	
			help				= 	Command.HELP.getCommandName(),
			contacts_show		=	Command.CONTACTS_SHOW.getCommandName(),
			contacts_search		=	Command.CONTACTS_SEARCH.getCommandName(),
			contacts_remove		=	Command.CONTACTS_REMOVE.getCommandName(),
			contacts_add		=	Command.CONTACTS_ADD.getCommandName(),
			contacts_update		=	Command.CONTACTS_UPDATE.getCommandName(),
			contacts_showpk		=	Command.CONTACTS_SHOWPK.getCommandName(),
			connections_add		=	Command.CONNECTIONS_ADD.getCommandName(),
			connections_remove	=	Command.CONNECTIONS_REMOVE.getCommandName(),
			connections_show	=	Command.CONNECTIONS_SHOW.getCommandName(),
			connect_to			=	Command.CONNECT_TO.getCommandName(),
			wait_for			=	Command.WAIT_FOR.getCommandName(),
			connections_close	=	Command.CONNECTIONS_CLOSE.getCommandName(),
			hello_world			=	Command.HELLO_WORLD.getCommandName();

	private final String NO_KEY = "";
	
	/** The CommunicationList of the QNCC, used for some tests */
	private static CommunicationList commList;
	/** The ConnectionManager, used for some tests, specifically regarding the connection commands */
	private static ConnectionManager conMan;
	
	private static ArrayList<Contact> backups;
	
	/*
	 * NOTE / TODO:
	 * Currently, these tests use the same CommunicationList that
	 * the actual user of the program accesses. This is not optimal,
	 * because they clear the list. Should either automatically back up the
	 * list before peforming the tests, or implement a way to have multiple lists.
	 */
	
	@BeforeAll
	static void initialize() {
		QuantumnetworkControllcenter.initialize(null);
		commList = QuantumnetworkControllcenter.communicationList;
		conMan	 = QuantumnetworkControllcenter.conMan;
		
		// back up the entries of the communication list, so that these tests don't destroy them
		backups = commList.queryAll();
		helper_clear_commList(commList);
	}
	
	@AfterAll
	static void restoreOldList() {
		for (Contact c : backups) {
			commList.insert(c.getName(), c.getIpAddress(), c.getPort(), c.getSignatureKey());
		}
	}
	
	/**
	 * Deletes all entries in the given CommunicationList.
	 * @param cl
	 * 		the CommunicationList to clear
	 */
	private static void helper_clear_commList(CommunicationList cl) {
		ArrayList<Contact> entries = cl.queryAll();
		if(entries != null) {
			for (Contact entry : entries) {
				cl.delete(entry.getName());
			}
		}
	}
	
	@Nested
	class invalid_inputs {
		
		@Test
		void null_input_throws_exception() {
			IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {CommandHandler.processCommand(null);});
			assertEquals(thrown.getMessage(), CommandHandler.NULL_COMMAND); // Correct error message is given
		}
		
		@Test
		void empty_input_is_invalid_command() {
			assertTrue(CommandHandler.processCommand("").contains("ERROR - UNRECOGNIZED COMMAND"));
		}
		
		@Test
		void garbage_input_is_invalid_command() {
			assertTrue(CommandHandler.processCommand("sdnsdvsdje").contains("ERROR - UNRECOGNIZED COMMAND"));
		}
		
	}
	
	@Nested
	class help {
		
		@Test
		void help_lists_all_commands() {
			String helpOutput = CommandHandler.processCommand("help");
			for(Command c : Command.values()) {
				assertTrue(helpOutput.contains(c.getCommandName()));
			}
		}
		
		@Test
		void help_help_works() {
			assertTrue(CommandHandler.processCommand("help help").contains(Command.HELP.getHelp()));
			assertTrue(CommandHandler.processCommand("     help   help   ").contains(Command.HELP.getHelp()));; // varying whitespace version
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		void help_for_individual_commands_works(Command c) {
			assertTrue(CommandHandler.processCommand(help + " " + c.getCommandName()).contains(c.getHelp()));		
		}
		
		@Test
		void help_for_individual_commands_is_argument_tolerant() {
			// "help <commandName> <args>" should work the same as "help <commandName>" regardless of <args>
			// This is particularly important because a user may not know the correct syntax
			assertTrue(CommandHandler.processCommand(help + " " + contacts_add 	+ " name=Alexa ip=127.0.1.1 port=4444").contains(Command.CONTACTS_ADD.getHelp()));
			assertTrue(CommandHandler.processCommand(help + " " + contacts_remove 	+ " Jamie").contains(Command.CONTACTS_REMOVE.getHelp()));
			assertTrue(CommandHandler.processCommand(help + " " + contacts_show 	+ " all").contains(Command.CONTACTS_SHOW.getHelp()));
			assertTrue(CommandHandler.processCommand(help + " " + contacts_update 	+ " name=Alexa to name=Bobbie").contains(Command.CONTACTS_UPDATE.getHelp()));
			assertTrue(CommandHandler.processCommand(help + " " + contacts_update 	+ " pk Alicia remove").contains(Command.CONTACTS_UPDATE.getHelp()));
			assertTrue(CommandHandler.processCommand(help + " " + contacts_search 	+ " ip=102.35.122.49").contains(Command.CONTACTS_SEARCH.getHelp()));
		}
		
		@Test
		void help_for_gibberish_outputs_no_help_can_be_provided() {
			String out = CommandHandler.processCommand("help udvusdeujfeifeir");
			assertTrue(out.contains("not a valid command"));
		}
		
		@Test
		void help_with_all_commands_has_short_help_for_each() {
			String helpOutput = CommandHandler.processCommand("help");
			for(Command c : Command.values()) {
				assertTrue(helpOutput.contains(c.getShortHelp()));
			}
		}
	}
	
	@Nested
	class Test_CommunicationList_Commands {
		
		@BeforeEach
		void before_each () {
			// Ensure the CommunicationList is empty
			helper_clear_commList(commList);
		}
		
		@AfterAll
		static void after_each() {
			// Do not leave any entries in the Database
			helper_clear_commList(commList);
		}
		
		@Test
		void contacts_add_works() {
			
			CommandHandler.processCommand(contacts_add + " Alexa 127.0.0.1 4444");
			CommandHandler.processCommand(contacts_add + " Billie 138.0.0.5 9999");
			CommandHandler.processCommand(contacts_add + " Charlie 168.0.1.7 26665");
			
			Contact Alexa 	= commList.query("Alexa");
			Contact Billie 	= commList.query("Billie");
			Contact Charlie = commList.query("Charlie");
			
			assertTrue(commList.queryAll().size() == 3);
			
			assertEquals(Alexa.getIpAddress(), "127.0.0.1");
			assertEquals(Billie.getIpAddress(), "138.0.0.5");
			assertEquals(Charlie.getIpAddress(), "168.0.1.7");
			
			assertEquals(Alexa.getPort(), 4444);
			assertEquals(Billie.getPort(), 9999);
			assertEquals(Charlie.getPort(), 26665);
		
		}
		
		@Test
		void contacts_remove_works() {

			commList.insert("Alexa", "127.0.0.1", 1, NO_KEY);
			commList.insert("Billie", "127.0.0.2", 2, NO_KEY);
			commList.insert("Charlie", "127.0.0.3", 3, NO_KEY);

			CommandHandler.processCommand(contacts_remove + " Alexa");
			CommandHandler.processCommand(contacts_remove + " Billie");
			CommandHandler.processCommand(contacts_remove + " Charlie");
			
			assertTrue(commList.queryAll().size() == 0);
			
		}
		
		@Test
		void contacts_show_works() {

			commList.insert("Alexa", "127.0.0.1", 1111, NO_KEY);
			commList.insert("Billie", "127.0.0.2", 2222, NO_KEY);
			commList.insert("Charlie", "127.0.0.3", 3333, NO_KEY);
			
			String shownContacts = CommandHandler.processCommand(contacts_show);
			
			assertTrue(shownContacts.contains("Alexa"));
			assertTrue(shownContacts.contains("Billie"));
			assertTrue(shownContacts.contains("Charlie"));
			
			assertTrue(shownContacts.contains("127.0.0.1"));
			assertTrue(shownContacts.contains("127.0.0.2"));
			assertTrue(shownContacts.contains("127.0.0.3"));
			
			assertTrue(shownContacts.contains("1111"));
			assertTrue(shownContacts.contains("2222"));
			assertTrue(shownContacts.contains("3333"));
		}

		@Test
		void contacts_update_works_for_names() {
			commList.insert("Alexa", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand(contacts_update + " Alexa name Bob");

			assertTrue(commList.queryAll().size() == 1);
			assertNull(commList.query("Alexa"));
			assertEquals("127.0.0.1", commList.query("Bob").getIpAddress());
			assertEquals(1111, commList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_ips() {
			commList.insert("Bob", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand(contacts_update + " Bob IP 168.0.0.1");

			assertTrue(commList.queryAll().size() == 1);
			assertEquals("168.0.0.1", commList.query("Bob").getIpAddress());
			assertEquals(1111, commList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_ports() {
			commList.insert("Bob", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand(contacts_update + " Bob Port 5555");

			assertTrue(commList.queryAll().size() == 1);
			assertEquals("127.0.0.1", commList.query("Bob").getIpAddress());
			assertEquals(5555, commList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_pks() {
			commList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand(contacts_update + " Alicia pk \"pkForTesting_1.pub\"");

			// Assert that # of entries, Name, IP and Port remain unchanged
			helper_Alicia_did_not_change();

			// Assert that pk was properly set
			Contact Alicia = commList.query("Alicia");
			assertEquals(SHA256withRSAAuthentication.readKeyStringFromFile("pkForTesting_1.pub"), Alicia.getSignatureKey());

			// Now check if pk can be deleted
			System.out.println(CommandHandler.processCommand(contacts_update + " Alicia pk remove"));
			Alicia = commList.query("Alicia"); // re-query needed! If we forget this, the test throws an error, because the object would still be the old result!
			assertEquals(NO_KEY, Alicia.getSignatureKey());

			// TODO: There needs to be a publicly accessible "default" entry for an unset key in a constant somewhere sensible
			// Possibly put it in the CommunicationList interface once that's finished
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_name() {
			commList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such names being alphanumeric, _ and - only
			 */
			
			// Invalid name
			CommandHandler.processCommand(contacts_update + " Alicia name B��-$$as");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia name M�r�<m");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia name ?_++qq&");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_ip() {
			commList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such as 1234 being an invalid IP
			 */
			
			// Invalid IP
			CommandHandler.processCommand(contacts_update + " Alicia ip 555.555.555");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia ip 1234");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia ip potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_port() {
			commList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such as a max port of 65535
			 */
			
			// Invalid Port
			CommandHandler.processCommand(contacts_update + " Alicia port 1000000000");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia port ??92394..");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia port potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_pk() {
			commList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			// Invalid PK
			CommandHandler.processCommand(contacts_update + " Alicia pk \"\"");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia pk \"this_file_does_not_exist.png.jpg.gif.mp3\"");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand(contacts_update + " Alicia pk potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		/**
		 * Helper Method for testing that update does not change the communication list when it is not supposed to.
		 * Checks that the communication list has exactly one entry, with the name "Alicia", ip "127.0.0.1" and Port 1111
		 * Does not check the pk associated with that entry in any way
		 */
		private void helper_Alicia_did_not_change() {
			assertTrue(commList.queryAll().size() == 1);
			Contact Alicia = commList.query("Alicia");
			assertNotNull(Alicia);
			assertEquals("127.0.0.1", Alicia.getIpAddress());
			assertEquals(1111, Alicia.getPort());
		}
		
		@Test
		void contacts_search_works() {

			commList.insert("Alexa", "127.0.0.1", 45345, NO_KEY);
			
			String searched = CommandHandler.processCommand(contacts_search + " Alexa");
			
			assertTrue(searched.contains("Alexa"));
			assertTrue(searched.contains("127.0.0.1"));
			assertTrue(searched.contains("45345"));

		}
		
		@Test
		void contacts_show_pk_works() {
			String exampleKey = "BLABLAKEYBLABLA123";
			String exampleKey2 = "XXXXXXXXXXXXXXXXX";
			commList.insert("Alexa", "127.0.0.1", 45345, exampleKey);
			
			String shouldContainKey = CommandHandler.processCommand(contacts_showpk + " Alexa");
			assertTrue(shouldContainKey.contains(exampleKey));
			assertFalse(shouldContainKey.contains(exampleKey2));
				
			commList.updateSignatureKey("Alexa", exampleKey2);
			String shouldContainKey2 = CommandHandler.processCommand(contacts_showpk + " Alexa");
			assertFalse(shouldContainKey2.contains(exampleKey));
			assertTrue(shouldContainKey2.contains(exampleKey2));
			
			commList.updateSignatureKey("Alexa", "");
			String shouldContainNeitherKey = CommandHandler.processCommand(contacts_showpk + " Alexa");
			assertFalse(shouldContainNeitherKey.contains(exampleKey));
			assertFalse(shouldContainNeitherKey.contains(exampleKey));
			
			
			
		}
		
		@Test
		void contacts_show_informs_user_in_edge_cases() { 
			/*
			 * NOTE: These tests might easily break if some string literals are changed,
			 * which is not optimal (although it may still warn the developer if they forgot something...).
			 * Not sure how else to automatically test for users being informed 
			 * about a contact having no pk, or contact not existing.
			 */
			commList.insert("Alexa", "127.0.0.1", 45345, "");
			
			// user should be informed that Alexa currently has no public key associated
			String shouldContainNoPublicKey = CommandHandler.processCommand(contacts_showpk + " Alexa");
			assertTrue(shouldContainNoPublicKey.contains("no public key"));
						
			// if trying to see the pk of 
			commList.delete("Alexa");
			String shouldInformUserNoSuchContact = CommandHandler.processCommand(contacts_showpk + " Alexa");
			assertTrue(shouldInformUserNoSuchContact.contains("no such contact"));
		}
		
	}


	@Nested
	class Test_Connection_Commands {
		
		final int 	ALICE_REMOTE_PORT 	= 10501,
					BOB_REMOTE_PORT		= 10502;
		
		/*
		 * Currently (14.02.2022) only connections to partners in the CommunicationList are allowed.
		 * So, we again need a clear list between each test case to not have any issues.
		 */
		
		@BeforeEach
		void beforeEach() {
			helper_clear_commList(commList);
			conMan.destroyAllConnectionEndpoints();
			commList.insert("Alice", 	conMan.getLocalAddress(), ALICE_REMOTE_PORT, NO_KEY);
			commList.insert("Bob", 		conMan.getLocalAddress(), BOB_REMOTE_PORT, NO_KEY);
		}
		
		@AfterAll
		static void afterAll() {
			helper_clear_commList(commList);
			conMan.destroyAllConnectionEndpoints();
		}
		
		/*
		 * Section for testing commands when given valid inputs.
		 */
		
		@ParameterizedTest
		@ValueSource(strings = {"Alice", "Bob"})
		void can_add_connection_with_default_port(String contactName) {	
			CommandHandler.processCommand(connections_add + " " + contactName);
			
			Map<String, ConnectionEndpoint> allEndpoints = conMan.returnAllConnections();
			ConnectionEndpoint endpoint = conMan.getConnectionEndpoint(contactName);
			
			assertEquals(1, allEndpoints.size(), "Expected only one ConnectionEndpoint to be added.");
			assertNotNull(endpoint, "Expected ConnectionEndpoint with ID of the contact to be added.");
			assertEquals(ConnectionCommandHandler.DEFAULT_PORT, endpoint.getServerPort(), "Expected newly created CE to listen on default port.");	
		}
		
		@ParameterizedTest
		@CsvSource({
			"Alice, 12345",
			"Bob, 5678"
		})
		void can_add_connection_with_custom_port(String contactName, int localPort) {
			CommandHandler.processCommand(connections_add + " " + contactName + " " + localPort);
			
			Map<String, ConnectionEndpoint> allEndpoints = conMan.returnAllConnections();
			ConnectionEndpoint endpoint = conMan.getConnectionEndpoint(contactName);
			
			assertEquals(1, allEndpoints.size(), "Expected only one ConnectionEndpoint to be added.");
			assertNotNull(endpoint, "Expected ConnectionEndpoint with ID of the contact to be added.");
			assertEquals(localPort, endpoint.getServerPort(), "Expected newly created CE to listen on specified port.");	
		}
		
		@Test
		void can_remove_connection() {			
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 12345);
			// conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 44400);
			
			assertEquals(2, conMan.returnAllConnections().size(), "Something went wrong while adding the connections to be deleted.");
			
			CommandHandler.processCommand(connections_remove + " Alice");
			
			assertEquals(1, conMan.returnAllConnections().size(), "One connection should have been removed.");
			assertNull(conMan.getConnectionEndpoint("Alice"), "There should be no connection with ID Alice anymore.");
			
			CommandHandler.processCommand(connections_remove + " Bob");

			assertEquals(0, conMan.returnAllConnections().size(), "All connections should have been removed.");
			assertNull(conMan.getConnectionEndpoint("Bob"), "There should be no connection with ID Bob anymore.");
		}
		
		@Test
		void can_show_connections() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1",12345);
			// conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 44400);
			
			String connectionTable =  CommandHandler.processCommand(connections_show);
			
			// Don't check for everything, but check for a few key contents of the string
			assertTrue(connectionTable.contains("Alice"), "Alice should be in the list of connections.");
			assertTrue(connectionTable.contains("Bob"), "Bob should be in the list of connections.");
			assertTrue(connectionTable.contains("" + ALICE_REMOTE_PORT), "Alice remote port should be in the list of connections.");
			assertTrue(connectionTable.contains("" + BOB_REMOTE_PORT), "Bob's remote port should be in the list of connections.");
			assertTrue(connectionTable.contains("" + 12345), "Alice local port should be in the list of connections.");
			assertTrue(connectionTable.contains("" + 44400), "Bob's local port should be in the list of connections.");			
		}
		
		@Test
		void can_make_connection_wait() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 12345);
			// conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 23123);
			
			CommandHandler.processCommand(wait_for + " Alice");	
			// assertEquals(ConnectionState.WAITINGFORCONNECTION, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.CLOSED, conMan.getConnectionState("Bob"));
			
			
			CommandHandler.processCommand(wait_for + " Bob");	
			// assertEquals(ConnectionState.WAITINGFORCONNECTION, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.WAITINGFORCONNECTION, conMan.getConnectionState("Bob"));
		}
		
		@Test
		void can_make_connection_send_request() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 12345);
			// conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 23123);
			
			CommandHandler.processCommand(connect_to + " Alice");	
			// assertEquals(ConnectionState.CONNECTING, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.CLOSED, conMan.getConnectionState("Bob"));
			
			
			CommandHandler.processCommand(connect_to + " Bob");	
			// assertEquals(ConnectionState.CONNECTING, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.CONNECTING, conMan.getConnectionState("Bob"));
		}
		
		@Test
		void cyclical_hello_world_works() throws InterruptedException {
			int NETWORK_DELAY_MS = 500;
			
			helper_clear_commList(commList);
			// "Alice" Side:
			 // Alice knows Bob wants to receive messages on Port 55020
			 commList.insert("Bob", "127.0.0.1", 55020, NO_KEY);
			 // Alice wants to receive messages from Bob on port 55010
			 // conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 55010);
			 // Alice will wait for Bob
			 // conMan.getConnectionEndpoint("Bob").waitForConnection();
			
			// "Bob" Side:
			 // Bob knows Alice wants to receive messages on Port 55010
			 commList.insert("Alice", "127.0.0.1", 55010, NO_KEY);
			 // Bob wants to receive messages from Alice on port 55020
			 // conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 55020);
			 // Bob sends a request for connection
			 try {
				conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 55010);
			 } catch (IOException e) {	
				assertTrue(false, "Something went wrong trying to connect. " + e.getMessage());
			 }
			 
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			 
			// Make sure they are connected
			// assertEquals(ConnectionState.CONNECTED, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.CONNECTED, conMan.getConnectionState("Bob"));
			 
			// Now both send each other a hello world
			
			// Alice sends a hello world to Bob
			CommandHandler.processCommand(hello_world + " Bob");
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			// Bob should have received a message on his CE to Alice
			// assertEquals(1, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue(), "Bob should have received a message from Alice.");
			// Alice should not have gotten any messages from Bob yet
			// assertEquals(0, conMan.getConnectionEndpoint("Bob").sizeOfMessageQueue(), "Alice should not have received a message from Bob.");

			
			// Bob sends a hello world back to Alice
			CommandHandler.processCommand(hello_world + " Alice");
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			// Now both should have exactly one message waiting
			// assertEquals(1, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue(), "Bob should have received a message from Alice.");
			// assertEquals(1, conMan.getConnectionEndpoint("Bob").sizeOfMessageQueue(), "Alice should have received a message from Bob.");
			
		}
		
		@Test
		void can_create_cyclical_connection_with_commands_only() throws InterruptedException {
			int NETWORK_DELAY_MS = 500;
			
			helper_clear_commList(commList);
			// "Alice" Side:
			 // Alice knows Bob wants to receive messages on Port 55020
			 CommandHandler.processCommand(contacts_add + " Bob 127.0.0.1 55020");
			 // Alice wants to receive messages from Bob on port 55010
			 CommandHandler.processCommand(connections_add + " Bob 55010");
			 // Alice will wait for Bob
			 CommandHandler.processCommand(wait_for + " Bob");
			
			// "Bob" Side:
			 // Bob knows Alice wants to receive messages on Port 55010
			 CommandHandler.processCommand(contacts_add + " Alice 127.0.0.1 55010");
			 // Bob wants to receive messages from Alice on port 55020
			 CommandHandler.processCommand(connections_add + " Alice 55020");
			 // Bob sends a request for connection
			 CommandHandler.processCommand(connect_to + " Alice");
			 
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			 
			// Make sure they are connected
			// assertEquals(ConnectionState.CONNECTED, conMan.getConnectionState("Alice"));
			// assertEquals(ConnectionState.CONNECTED, conMan.getConnectionState("Bob"));
			 
			// Now both send each other a hello world
			
			// Alice sends a hello world to Bob
			CommandHandler.processCommand(hello_world + " Bob");
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			// Bob should have received a message on his CE to Alice
			// assertEquals(1, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue(), "Bob should have received a message from Alice.");
			// Alice should not have gotten any messages from Bob yet
			// assertEquals(0, conMan.getConnectionEndpoint("Bob").sizeOfMessageQueue(), "Alice should not have received a message from Bob.");

			
			// Bob sends a hello world back to Alice
			CommandHandler.processCommand(hello_world + " Alice");
			TimeUnit.MILLISECONDS.sleep(NETWORK_DELAY_MS);
			// Now both should have exactly one message waiting
			// assertEquals(1, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue(), "Bob should have received a message from Alice.");
			// assertEquals(1, conMan.getConnectionEndpoint("Bob").sizeOfMessageQueue(), "Alice should have received a message from Bob.");			
		}
		
		/*
		 * Section for testing commands when given invalid inputs.
		 */
		
		@Test
		void add_connection_adds_nothing_for_invalid_inputs() {
			CommandHandler.processCommand(connections_add + " ");
			assertEquals(0, conMan.returnAllConnections().size(), "No contact name / connection ID was specified, so no connection should have been added. ");
			CommandHandler.processCommand(connections_add + " Clarence");
			assertEquals(0, conMan.returnAllConnections().size(), "There is no contact named Clarence in the list, so no connection should have been added. ");
			CommandHandler.processCommand(connections_add + " Alice 65536");
			assertEquals(0, conMan.returnAllConnections().size(), "Port 65536 is not a valid TCP Port, so no connection should have been added. ");
			CommandHandler.processCommand(connections_add + " Alice -100");
			assertEquals(0, conMan.returnAllConnections().size(), "Port -100 is not a valid TCP Port, so no connection should have been added. ");			
			CommandHandler.processCommand(connections_add + " Alice 127.0.0.1 55555");
			assertEquals(0, conMan.returnAllConnections().size(), "Adding a connection requires only the name of a contact and optionally a port. ");
		}
		
		@Test
		void add_connections_does_not_work_for_same_contact_twice() {
			// Can't add two connections for one contact
			CommandHandler.processCommand(connections_add + " Alice 55010");
			CommandHandler.processCommand(connections_add + " Alice 55020");
			assertEquals(1, conMan.returnAllConnections().size(), "Shouldn't have been able to add two connections for contact Alice. ");
		}
		
		@Test
		void add_connection_does_not_work_if_port_is_in_use() {
			// Can't add two connections listening on the same port
			CommandHandler.processCommand(connections_add + " Alice 55040");
			CommandHandler.processCommand(connections_add + " Bob 55040");
			assertEquals(1, conMan.returnAllConnections().size(), "Shouldn't have been able to add two connections listening on the same port. ");
		}
		
		@Test
		void remove_connection_removes_nothing_for_invalid_inputs() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 55500);
			CommandHandler.processCommand(connections_remove);
			assertEquals(1, conMan.returnAllConnections().size(), "Should not have removed any connections. ");
			CommandHandler.processCommand(connections_remove + " Bob");
			assertEquals(1, conMan.returnAllConnections().size(), "There is no connection for Bob - should not have removed any connections. ");
			CommandHandler.processCommand(connections_remove + " Alice 55500");
			assertEquals(1, conMan.returnAllConnections().size(), "Removing a connection only requires the name. ");
		}
		
		@Test
		void wait_for_connection_behaves_well_for_invalid_inputs() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 55500);
			CommandHandler.processCommand(wait_for);
			assertEquals(ConnectionState.CLOSED, conMan.getConnectionEndpoint("Alice").reportState(), "CE Alice state should only change if it is specified to wait for CE Alice.");
			CommandHandler.processCommand(wait_for + " Bob");
			assertEquals(ConnectionState.CLOSED, conMan.getConnectionEndpoint("Alice").reportState(), "CE Alice state should only change if it is specified to wait for CE Alice.");
		}
		
		@Test
		void can_not_close_connection_that_is_not_open() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 55500);
			// conMan.getConnectionEndpoint("Alice").waitForConnection();
			
			CommandHandler.processCommand(connections_close);
			assertEquals(ConnectionState.WAITINGFORCONNECTION, conMan.getConnectionEndpoint("Alice").reportState(), "Alice was closed even though no CE was specified.");
			CommandHandler.processCommand(connections_close + " Bob");
			assertEquals(ConnectionState.WAITINGFORCONNECTION, conMan.getConnectionEndpoint("Alice").reportState(), "Alice was closed even though Bob was specified as the connection to close.");
		}
		
		@Test
		void hello_world_with_invalid_input_behaves_well() {
			// conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 55500);
			
			CommandHandler.processCommand(hello_world);
			// assertEquals(0, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue());
			CommandHandler.processCommand(hello_world + " Bob");
			// assertEquals(0, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue());
			CommandHandler.processCommand(hello_world + " Alice"); 
			// assertEquals(0, conMan.getConnectionEndpoint("Alice").sizeOfMessageQueue());
		
		}
		
	}
	

}
