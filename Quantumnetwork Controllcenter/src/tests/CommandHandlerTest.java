package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import CommunicationList.Database;
import CommunicationList.DbObject;
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
			assertEquals(Command.CONTACTS_SEARCH.getHelp(), CommandHandler.processCommand("help contacts search ip=102.35.122.49"));
		}
		
	}
	
	@Nested
	class communicationList_Commands{
		
		@BeforeEach
		void before_each () {
			// Ensure that the Database is clear of all entries
			ArrayList<DbObject> entries = Database.queryAll();
			for (DbObject entry : entries) {
				Database.delete(entry.getName());
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
			
			assertEquals(Alexa.getIpAddr(), "127.0.0.1");
			assertEquals(Billie.getIpAddr(), "138.0.0.5");
			assertEquals(Charlie.getIpAddr(), "168.0.1.7");
			
			assertEquals(Alexa.getPort(), "4444");
			assertEquals(Billie.getPort(), "9999");
			assertEquals(Charlie.getPort(), "26665");
		
		}
		
		@Test
		void contacts_remove_works() {
			
			Database.insert("Alexa", "127.0.0.1", 1);
			Database.insert("Billie", "127.0.0.2", 2);
			Database.insert("Charlie", "127.0.0.3", 3);
			
			CommandHandler.processCommand("contacts remove Alexa");
			CommandHandler.processCommand("contacts remove Billie");
			CommandHandler.processCommand("contacts remove Charlie");
			
			assertTrue(Database.queryAll().size() == 0);
			
		}
		
		@Test
		void contacts_show_works() {
			
			Database.insert("Alexa", "127.0.0.1", 1);
			Database.insert("Billie", "127.0.0.2", 2);
			Database.insert("Charlie", "127.0.0.3", 3);
			
			String shownContacts = CommandHandler.processCommand("contacts show");
			
			assertTrue(shownContacts.contains("Alexa"));
			assertTrue(shownContacts.contains("Billie"));
			assertTrue(shownContacts.contains("Charlie"));
			
			assertTrue(shownContacts.contains("127.0.0.1"));
			assertTrue(shownContacts.contains("127.0.0.2"));
			assertTrue(shownContacts.contains("127.0.0.3"));
		}
		
		@Test
		void contacts_update_works() {
		
			Database.insert("Alexa", "127.0.0.1", 1);
			
			CommandHandler.processCommand("contacts update Alexa name Bob");
			
			assertTrue(Database.queryAll().size() == 1);
			assertNull(Database.query("Alexa"));
			assertEquals(Database.query("Bob").getIpAddr(), "127.0.0.1");
			assertEquals(Database.query("Bob").getPort(), "1");
			
			CommandHandler.processCommand("contacts update Bob IP 168.0.0.1");
			
			assertTrue(Database.queryAll().size() == 1);
			assertEquals(Database.query("Bob").getIpAddr(), "168.0.0.1");
			assertEquals(Database.query("Bob").getPort(), "1");
			
			CommandHandler.processCommand("contacts update Bob Port 5555");
			
			assertTrue(Database.queryAll().size() == 1);
			assertEquals(Database.query("Bob").getIpAddr(), "168.0.0.1");
			assertEquals(Database.query("Bob").getPort(), "5555");
			
		}
		
		@Test
		void contacts_search_works() {
			
			Database.insert("Alexa", "127.0.0.1", 45345);
			
			String searched = CommandHandler.processCommand("contacts search Alexa");
			
			assertTrue(searched.contains("Alexa"));
			assertTrue(searched.contains("127.0.0.1"));
			assertTrue(searched.contains("45345"));

		}
		
	}

	
}
