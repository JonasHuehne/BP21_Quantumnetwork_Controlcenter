import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import messengerSystem.SHA256withRSAAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.QuantumnetworkControllcenter;
import ui.Command;
import ui.CommandHandler;

/**
 * This class is used to perform unit tests for the {@link CommandHandler} class,
 * specifically to see if it can deal with unexpected inputs,
 * and if the application behaves as expected on regular inputs
 * (e.g. it is possible to send a message between two nodes with the correct command).
 * 
 * Please keep in mind that errors of the tests performed here may not neccessarily mean that 
 * the {@link CommandHandler} class is broken, it could be that the methods the {@link CommandHandler} is calling are broken.
 * 
 * @author Sasha Petri
 */
class CommandHandlerTest {

	private final String NO_KEY = "";
	CommunicationList db;
	
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
			assertEquals(Command.HELP.getHelp(), CommandHandler.processCommand("help help"));
			assertEquals(Command.HELP.getHelp(), CommandHandler.processCommand("	help 	  help	")); // varying whitespace version
		}
		
		@Test
		void help_for_communication_list_command_works() {
			assertEquals(Command.CONTACTS_ADD.getHelp(), CommandHandler.processCommand("help contacts add"));
			assertEquals(Command.CONTACTS_REMOVE.getHelp(), CommandHandler.processCommand("help contacts remove"));
			assertEquals(Command.CONTACTS_SHOW.getHelp(), CommandHandler.processCommand("help contacts show"));
			assertEquals(Command.CONTACTS_UPDATE.getHelp(), CommandHandler.processCommand("help contacts update"));
			assertEquals(Command.CONTACTS_SEARCH.getHelp(), CommandHandler.processCommand("help contacts search"));
			
			// Non-Strict versions (i.e. more syntax than just the command name)
			
			assertEquals(Command.CONTACTS_ADD.getHelp(), CommandHandler.processCommand("help contacts add name=Alexa ip=127.0.1.1 port=4444"));
			assertEquals(Command.CONTACTS_REMOVE.getHelp(), CommandHandler.processCommand("help contacts remove Jamie"));
			assertEquals(Command.CONTACTS_SHOW.getHelp(), CommandHandler.processCommand("help contacts show all"));
			assertEquals(Command.CONTACTS_UPDATE.getHelp(), CommandHandler.processCommand("help contacts update name=Alexa to name=Bobbie"));
			assertEquals(Command.CONTACTS_UPDATE.getHelp(), CommandHandler.processCommand("help contacts update pk Alicia remove"));
			assertEquals(Command.CONTACTS_SEARCH.getHelp(), CommandHandler.processCommand("help contacts search ip=102.35.122.49"));
		}
		
		@Test
		void help_for_gibberish_outputs_no_help_can_be_provided() {
			String out = CommandHandler.processCommand("help udvusdeujfeifeir");
			assertTrue(out.contains("not a valid command"));
		}
	}
	
	@Nested
	class communicationList_Commands{
		
		@BeforeEach
		void before_each () {
			// initialize the database
			QuantumnetworkControllcenter.initialize();

			// Ensure that the Database is clear of all entries
			ArrayList<Contact> entries = QuantumnetworkControllcenter.communicationList.queryAll();
			if(entries != null) {
				for (Contact entry : entries) {
					QuantumnetworkControllcenter.communicationList.delete(entry.getName());
				}
			}
		}
		
		@AfterEach 
		void after_each() {
			// Do not leave any entries in the Database
			ArrayList<Contact> entries = QuantumnetworkControllcenter.communicationList.queryAll();
			if(entries != null) {
				for (Contact entry : entries) {
					QuantumnetworkControllcenter.communicationList.delete(entry.getName());
				}
			}
		}
		
		@Test
		void contacts_add_works() {
			
			CommandHandler.processCommand("contacts add Alexa 127.0.0.1 4444");
			CommandHandler.processCommand("contacts add Billie 138.0.0.5 9999");
			CommandHandler.processCommand("contacts add Charlie 168.0.1.7 26665");
			
			Contact Alexa = QuantumnetworkControllcenter.communicationList.query("Alexa");
			Contact Billie = QuantumnetworkControllcenter.communicationList.query("Billie");
			Contact Charlie = QuantumnetworkControllcenter.communicationList.query("Charlie");
			
			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 3);
			
			assertEquals(Alexa.getIpAddress(), "127.0.0.1");
			assertEquals(Billie.getIpAddress(), "138.0.0.5");
			assertEquals(Charlie.getIpAddress(), "168.0.1.7");
			
			assertEquals(Alexa.getPort(), 4444);
			assertEquals(Billie.getPort(), 9999);
			assertEquals(Charlie.getPort(), 26665);
		
		}
		
		@Test
		void contacts_remove_works() {

			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 1, NO_KEY);
			QuantumnetworkControllcenter.communicationList.insert("Billie", "127.0.0.2", 2, NO_KEY);
			QuantumnetworkControllcenter.communicationList.insert("Charlie", "127.0.0.3", 3, NO_KEY);

			CommandHandler.processCommand("contacts remove Alexa");
			CommandHandler.processCommand("contacts remove Billie");
			CommandHandler.processCommand("contacts remove Charlie");
			
			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 0);
			
		}
		
		@Test
		void contacts_show_works() {

			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 1111, NO_KEY);
			QuantumnetworkControllcenter.communicationList.insert("Billie", "127.0.0.2", 2222, NO_KEY);
			QuantumnetworkControllcenter.communicationList.insert("Charlie", "127.0.0.3", 3333, NO_KEY);
			
			String shownContacts = CommandHandler.processCommand("contacts show");
			
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
			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand("contacts update Alexa name Bob");

			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 1);
			assertNull(QuantumnetworkControllcenter.communicationList.query("Alexa"));
			assertEquals("127.0.0.1", QuantumnetworkControllcenter.communicationList.query("Bob").getIpAddress());
			assertEquals(1111, QuantumnetworkControllcenter.communicationList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_ips() {
			QuantumnetworkControllcenter.communicationList.insert("Bob", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand("contacts update Bob IP 168.0.0.1");

			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 1);
			assertEquals("168.0.0.1", QuantumnetworkControllcenter.communicationList.query("Bob").getIpAddress());
			assertEquals(1111, QuantumnetworkControllcenter.communicationList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_ports() {
			QuantumnetworkControllcenter.communicationList.insert("Bob", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand("contacts update Bob Port 5555");

			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 1);
			assertEquals("127.0.0.1", QuantumnetworkControllcenter.communicationList.query("Bob").getIpAddress());
			assertEquals(5555, QuantumnetworkControllcenter.communicationList.query("Bob").getPort());
		}

		@Test
		void contacts_update_works_for_pks() {
			QuantumnetworkControllcenter.communicationList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);

			CommandHandler.processCommand("contacts update Alicia pk \"pkForTesting_1.pub\"");

			// Assert that # of entries, Name, IP and Port remain unchanged
			helper_Alicia_did_not_change();

			// Assert that pk was properly set
			Contact Alicia = QuantumnetworkControllcenter.communicationList.query("Alicia");
			assertEquals(SHA256withRSAAuthentication.readPublicKeyStringFromFile("pkForTesting_1.pub"), Alicia.getSignatureKey());

			// Now check if pk can be deleted
			System.out.println(CommandHandler.processCommand("contacts update Alicia pk remove"));
			Alicia = QuantumnetworkControllcenter.communicationList.query("Alicia"); // re-query needed! If we forget this, the test throws an error, because the object would still be the old result!
			assertEquals(NO_KEY, Alicia.getSignatureKey());

			// TODO: There needs to be a publicly accessible "default" entry for an unset key in a constant somewhere sensible
			// Possibly put it in the CommunicationList interface once that's finished
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_name() {
			QuantumnetworkControllcenter.communicationList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such names being alphanumeric, _ and - only
			 */
			
			// Invalid name
			CommandHandler.processCommand("contacts update Alicia name B��-$$as");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia name M�r�<m");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia name ?_++qq&");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_ip() {
			QuantumnetworkControllcenter.communicationList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such as 1234 being an invalid IP
			 */
			
			// Invalid IP
			CommandHandler.processCommand("contacts update Alicia ip 555.555.555");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia ip 1234");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia ip potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_port() {
			QuantumnetworkControllcenter.communicationList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			/*
			 * Note: Errors in this test may not necessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such as a max port of 65535
			 */
			
			// Invalid IP
			CommandHandler.processCommand("contacts update Alicia port 1000000000");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia port ??92394..");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia port potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_pk() {
			QuantumnetworkControllcenter.communicationList.insert("Alicia", "127.0.0.1", 1111, NO_KEY);
			
			// Invalid IP
			CommandHandler.processCommand("contacts update Alicia pk \"\"");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia pk \"this_file_does_not_exist.png.jpg.gif.mp3\"");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia pk potatoes");
			helper_Alicia_did_not_change();
			
		}
		
		/**
		 * Helper Method for testing that update does not change the communication list when it is not supposed to.
		 * Checks that the communicationlist has exactly one entry, with the name "Alicia", ip "127.0.0.1" and Port 1111
		 * Does not check the pk associated with that entry in any way
		 */
		private void helper_Alicia_did_not_change() {
			assertTrue(QuantumnetworkControllcenter.communicationList.queryAll().size() == 1);
			Contact Alicia = QuantumnetworkControllcenter.communicationList.query("Alicia");
			assertNotNull(Alicia);
			assertEquals("127.0.0.1", Alicia.getIpAddress());
			assertEquals(1111, Alicia.getPort());
		}
		
		@Test
		void contacts_search_works() {

			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 45345, NO_KEY);
			
			String searched = CommandHandler.processCommand("contacts search Alexa");
			
			assertTrue(searched.contains("Alexa"));
			assertTrue(searched.contains("127.0.0.1"));
			assertTrue(searched.contains("45345"));

		}
		
		@Test
		void contacts_show_pk_works() {
			String exampleKey = "BLABLAKEYBLABLA123";
			String exampleKey2 = "XXXXXXXXXXXXXXXXX";
			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 45345, exampleKey);
			
			String shouldContainKey = CommandHandler.processCommand("contacts showpk Alexa");
			assertTrue(shouldContainKey.contains(exampleKey));
			assertFalse(shouldContainKey.contains(exampleKey2));
				
			QuantumnetworkControllcenter.communicationList.updateSignatureKey("Alexa", exampleKey2);
			String shouldContainKey2 = CommandHandler.processCommand("contacts showpk Alexa");
			assertFalse(shouldContainKey2.contains(exampleKey));
			assertTrue(shouldContainKey2.contains(exampleKey2));
			
			QuantumnetworkControllcenter.communicationList.updateSignatureKey("Alexa", "");
			String shouldContainNeitherKey = CommandHandler.processCommand("contacts showpk Alexa");
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
			QuantumnetworkControllcenter.communicationList.insert("Alexa", "127.0.0.1", 45345, "");
			
			// user should be informed that Alexa currently has no public key associated
			String shouldContainNoPublicKey = CommandHandler.processCommand("contacts showpk Alexa");
			assertTrue(shouldContainNoPublicKey.contains("no public key"));
						
			// if trying to see the pk of 
			QuantumnetworkControllcenter.communicationList.delete("Alexa");
			String shouldInformUserNoSuchContact = CommandHandler.processCommand("contacts showpk Alexa");
			assertTrue(shouldInformUserNoSuchContact.contains("no such contact"));
		}
		
	}

	
}
