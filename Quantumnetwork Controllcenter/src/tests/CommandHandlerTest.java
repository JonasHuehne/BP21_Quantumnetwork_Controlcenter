package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import CommunicationList.Database;
import CommunicationList.DbObject;
import MessengerSystem.Authentication;
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
		
	}
	
	@Nested
	class communicationList_Commands{
		
		@BeforeEach
		void before_each () {
			// Ensure that the Database is clear of all entries
			ArrayList<DbObject> entries = Database.queryAll();
			if(entries != null) {
				for (DbObject entry : entries) {
					Database.delete(entry.getName());
				}
			}
		}
		
		@AfterEach 
		void after_each() {
			// Do not leave any entries in the Database
			ArrayList<DbObject> entries = Database.queryAll();
			if(entries != null) {
				for (DbObject entry : entries) {
					Database.delete(entry.getName());
				}
			}
		}
		
		@Test
		void contacts_add_works() {
			
			CommandHandler.processCommand("contacts add Alexa 127.0.0.1 4444");
			CommandHandler.processCommand("contacts add Billie 138.0.0.5 9999");
			CommandHandler.processCommand("contacts add Charlie 168.0.1.7 26665");
			
			DbObject Alexa = Database.query("Alexa");
			DbObject Billie = Database.query("Billie");
			DbObject Charlie = Database.query("Charlie");
			
			assertTrue(Database.queryAll().size() == 3);
			
			assertEquals(Alexa.getIpAddress(), "127.0.0.1");
			assertEquals(Billie.getIpAddress(), "138.0.0.5");
			assertEquals(Charlie.getIpAddress(), "168.0.1.7");
			
			assertEquals(Alexa.getPort(), 4444);
			assertEquals(Billie.getPort(), 9999);
			assertEquals(Charlie.getPort(), 26665);
		
		}
		
		@Test
		void contacts_remove_works() {
			
			Database.insert("Alexa", "127.0.0.1", 1, "NO KEY SET");
			Database.insert("Billie", "127.0.0.2", 2, "NO KEY SET");
			Database.insert("Charlie", "127.0.0.3", 3, "NO KEY SET");
			
			CommandHandler.processCommand("contacts remove Alexa");
			CommandHandler.processCommand("contacts remove Billie");
			CommandHandler.processCommand("contacts remove Charlie");
			
			assertTrue(Database.queryAll().size() == 0);
			
		}
		
		@Test
		void contacts_show_works() {
			
			Database.insert("Alexa", "127.0.0.1", 1111, "NO KEY SET");
			Database.insert("Billie", "127.0.0.2", 2222, "NO KEY SET");
			Database.insert("Charlie", "127.0.0.3", 3333, "NO KEY SET");
			
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
			Database.insert("Alexa", "127.0.0.1", 1111, "NO KEY SET");
			
			CommandHandler.processCommand("contacts update Alexa name Bob");
			
			assertTrue(Database.queryAll().size() == 1);
			assertNull(Database.query("Alexa"));
			assertEquals("127.0.0.1", Database.query("Bob").getIpAddress());
			assertEquals(1111, Database.query("Bob").getPort());
		}
		
		@Test
		void contacts_update_works_for_ips() {
			Database.insert("Bob", "127.0.0.1", 1111, "NO KEY SET");
			
			CommandHandler.processCommand("contacts update Bob IP 168.0.0.1");
			
			assertTrue(Database.queryAll().size() == 1);
			assertEquals("168.0.0.1", Database.query("Bob").getIpAddress());
			assertEquals(1111, Database.query("Bob").getPort());		
		}
		
		@Test
		void contacts_update_works_for_ports() {
			Database.insert("Bob", "127.0.0.1", 1111, "NO KEY SET");
			
			CommandHandler.processCommand("contacts update Bob Port 5555");
			
			assertTrue(Database.queryAll().size() == 1);
			assertEquals("127.0.0.1", Database.query("Bob").getIpAddress());
			assertEquals(5555, Database.query("Bob").getPort());
		}
		
		@Test
		void contacts_update_works_for_pks() {
			Database.insert("Alicia", "127.0.0.1", 1111, "NO KEY SET");
			
			CommandHandler.processCommand("contacts update Alicia pk \"pkForTesting_1\"");
			
			// Assert that # of entries, Name, IP and Port remain unchanged
			helper_Alicia_did_not_change();
			
			// Assert that pk was properly set
			DbObject Alicia = Database.query("Alicia");
			assertEquals(Authentication.readPublicKeyStringFromFile("pkForTesting_1"), Alicia.getSignatureKey());
									
			// Now check if pk can be deleted
			System.out.println(CommandHandler.processCommand("contacts update Alicia pk remove"));
			Alicia = Database.query("Alicia"); // re-query needed! If we forget this, the test throws an error, because the object would still be the old result!
			assertEquals("NO KEY SET", Alicia.getSignatureKey());
			
			// TODO: There needs to be a publicly accessible "default" entry for an unset key in a constant somewhere sensible
			// Possibly put it in the CommunicationList interface once that's finished
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_name() {
			Database.insert("Alicia", "127.0.0.1", 1111, "NO KEY SET");
			
			/*
			 * Note: Errors in this test may not neccessarily be caused by implementation issues of CommandHandler
			 * It may be that the currently used CommunicationList simply does not enforce restrictions such names being alphanumeric, _ and - only
			 */
			
			// Invalid name
			CommandHandler.processCommand("contacts update Alicia name Böö-$$as");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia name Mörü<m");
			helper_Alicia_did_not_change();
			CommandHandler.processCommand("contacts update Alicia name ?_++qq&");
			helper_Alicia_did_not_change();
			
		}
		
		@Test
		void contacts_update_wrong_input_makes_no_changes_for_ip() {
			Database.insert("Alicia", "127.0.0.1", 1111, "NO KEY SET");
			
			/*
			 * Note: Errors in this test may not neccessarily be caused by implementation issues of CommandHandler
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
			Database.insert("Alicia", "127.0.0.1", 1111, "NO KEY SET");
			
			/*
			 * Note: Errors in this test may not neccessarily be caused by implementation issues of CommandHandler
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
			Database.insert("Alicia", "127.0.0.1", 1111, "NO KEY SET");
			
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
			assertTrue(Database.queryAll().size() == 1);
			DbObject Alicia = Database.query("Alicia");
			assertNotNull(Alicia);
			assertEquals("127.0.0.1", Alicia.getIpAddress());
			assertEquals(1111, Alicia.getPort());
		}
		
		@Test
		void contacts_search_works() {
			
			Database.insert("Alexa", "127.0.0.1", 45345, "NO KEY SET");
			
			String searched = CommandHandler.processCommand("contacts search Alexa");
			
			assertTrue(searched.contains("Alexa"));
			assertTrue(searched.contains("127.0.0.1"));
			assertTrue(searched.contains("45345"));

		}
		
	}

	
}
