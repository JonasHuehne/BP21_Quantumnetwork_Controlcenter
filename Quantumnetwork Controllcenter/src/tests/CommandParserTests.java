package tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ui.Command;
import ui.CommandParser;

/**
 * @author Sasha Petri
 */
class CommandParserTests {
	
	@Test
	void test_norm_input() {
		assertNull(CommandParser.normInput(null));
		assertEquals("abc", CommandParser.normInput("       abc     		"));
		assertEquals("a b c", CommandParser.normInput(" a	b		 c	 "));
	}
	
	@Nested
	class Test_GetCommandOfName {
		@Test
		void test_GetCommandOfName_normal() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName(Command.HELP.getCommandName(), true));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName(Command.CONTACTS_ADD.getCommandName(), true));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName(Command.CONTACTS_SHOW.getCommandName(), true));
		}
		
		@Test
		void test_GetCommandOfName_normal_nonstrict() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName(Command.HELP.getCommandName(), false));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName(Command.CONTACTS_ADD.getCommandName(), false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName(Command.CONTACTS_SHOW.getCommandName(), false));
		}
		
		@Test
		void test_GetCommandOfName_varying_whitespaces() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName("    help   ", true));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("       contacts      add 		", true));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("   contacts  show 		", true));
		}
		
		@Test
		void test_GetCommandOfName_varying_whitespaces_nonstrict() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName("    help   ", false));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("       contacts      add 		", false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("   contacts  show 		", false));
		}
		
		@Test
		void test_GetCommandOfName_case_sensitivity() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName("HELP", true));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("CONtaCTs ADd", true));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("cONTACTS sHOW", true));
		}
		
		@Test
		void test_GetCommandOfName_case_sensitivity_nonstrict() {
			assertEquals(Command.HELP, CommandParser.getCommandOfName("HELP", false));
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("CONtaCTs ADd", false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("cONTACTS sHOW", false));
		}
		
		@Test
		void test_GetCommandOfName_null() {
			assertNull(CommandParser.getCommandOfName(null, true));
			assertNull(CommandParser.getCommandOfName("dfvndfn34534vnnvn11fnnfnnaaaa", true));
		}
		
		@Test
		void test_GetCommandOfName_null_nonstrict() {
			assertNull(CommandParser.getCommandOfName(null, false));
			assertNull(CommandParser.getCommandOfName("dfvndfn34534vnnvn11fnnfnnaaaa", false));
		}
		
		@Test
		void test_GetCommandOfName_nonstrict_wrong_syntax()  {
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("contacts add Jimmy", false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("contacts show Annie", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Annie ip", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Cassie height 172cm", false));
		}
	}
	
	@Nested
	class Test_Match {
		@Test
		void match_outputs_null_for_invalid_inputs() {
			assertNull(CommandParser.match(null));
			assertNull(CommandParser.match("uwdfusew "));
			assertNull(CommandParser.match("hhelp"));
			assertNull(CommandParser.match("cotnacts add"));
		}

		@Test
		/**
		 * Check whether leading, trailing or multiple white spaces inhibit the ability of the CommandParser to match a text string to its command.
		 */
		void test_match_for_varying_whitespaces() {	
			assertEquals(Command.HELP, CommandParser.match("help"));
			assertEquals(Command.HELP, CommandParser.match("   help 		 "));
			
			assertEquals(Command.CONTACTS_ADD, CommandParser.match("contacts add Bob 127.0.0.1 80"));
			assertEquals(Command.CONTACTS_ADD, CommandParser.match("  contacts   	 add   Bob    127.0.0.1    	80  	"));
			
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Bob name Bobbie"));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts		 update	 Bob  	 name 	 	Bobbie 	 	"));
		}

		@Test
		/**
		 * Affirm that the command name is not case sensitive for matching.
		 */
		void test_case_sensitivity_for_match() {
			assertEquals(Command.HELP, CommandParser.match("HElP"));
			assertEquals(Command.CONTACTS_ADD, CommandParser.match("CoNTacTs AdD Bob 127.0.0.1 80"));
			assertEquals(Command.CONTACTS_REMOVE, CommandParser.match("contACTs REMOVE Bob"));
		}
		
		@Test
		void test_match_for_communication_list_commands_entered_correctly() {	
			assertEquals(Command.CONTACTS_ADD, CommandParser.match("contacts add Bob 127.0.0.1 80"));
			assertEquals(Command.CONTACTS_REMOVE, CommandParser.match("contacts remove Alice"));
			assertEquals(Command.CONTACTS_SEARCH, CommandParser.match("contacts search Bob"));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.match("contacts show"));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Alice name Bob"));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Cassie ip 168.0.0.4"));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Steve port 25565"));
		}
		
		@Test
		void test_match_for_communication_list_commands_entered_incorrectly() {	
			assertNull(CommandParser.match("contacts add Bob"));
			assertNull(CommandParser.match("contacts add Bob 127.0.0.1"));
			assertNull(CommandParser.match("contacts add Bob 127.0.0.1 "));
			assertNull(CommandParser.match("contacts add Bob 127.0.0.1 x"));
			
			assertNull(CommandParser.match("contacts remove"));
			assertNull(CommandParser.match("contacts remove "));
			// TODO: Currently, white spaces are allowed in names, however, if they are ever forbidden add these two lines
			// assertNull(CommandParser.match("contacts remove 	 ")); 
			// assertNull(CommandParser.match("contacts remove Alice Bob"));
			
			assertNull(CommandParser.match("contacts show all"));
			
			// TODO: Currently, there are few restrictions on valid symbols names and IP (in particular, whitespaces being allowed is problematic)
			// If they are ever removed, add in the commented-out test cases
			assertNull(CommandParser.match("contacts update"));
			assertNull(CommandParser.match("contacts update "));
			assertNull(CommandParser.match("contacts update name"));
			// assertNull(CommandParser.match("contacts update Bob name Alice ip 127.0.0.1"));
			// assertNull(CommandParser.match("contacts update Bob name ,,,$$--+*?"));
			assertNull(CommandParser.match("contacts update Alice ip "));
			// assertNull(CommandParser.match("contacts update Alice ip PotatoChips");
			assertNull(CommandParser.match("contacts update Cassie port Potato"));
			
			assertNull(CommandParser.match("     "));
		}
	}
	
	@Nested
	class Test_Extract_Arguments {
		@Test
		/**
		 * Asserts that extract arguments returns null for invalid commands or null inputs.
		 */
		void test_extract_arguments_for_invalid_commands() {
			assertNull(CommandParser.extractArguments(null));
			assertNull(CommandParser.extractArguments("    "));
			assertNull(CommandParser.extractArguments("34rgjdrgjergjer"));
			assertNull(CommandParser.extractArguments("contacts remove "));
			assertNull(CommandParser.extractArguments("contacts update Cassie port Potato"));
		}
		
		@Test
		void test_extract_arguments_for_help() {
			
			Assertions.assertArrayEquals(new String[]{}, CommandParser.extractArguments("help"));
			Assertions.assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments("help help"));
			Assertions.assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments("help 		help 	"));
			
			/* 
			 * Help is allowed to be followed by any String syntactically. 
			 * The semantics of help only giving a valid output if followed by nothing or an actual command are not the job of the CommandParser. 
			 */
			
			Assertions.assertArrayEquals(new String[]{"contacts", "add"}, CommandParser.extractArguments("help contacts add"));
			Assertions.assertArrayEquals(new String[]{"contacts", "remove"}, CommandParser.extractArguments("help contacts remove"));
			Assertions.assertArrayEquals(new String[]{"contacts", "show"}, CommandParser.extractArguments("help contacts show"));
			
			Assertions.assertArrayEquals(new String[]{"contacts", "add"}, CommandParser.extractArguments("		help 	 contacts 	add		"));
			Assertions.assertArrayEquals(new String[]{"contacts", "remove"}, CommandParser.extractArguments("     help  		contacts 	remove   "));
			Assertions.assertArrayEquals(new String[]{"contacts", "show"}, CommandParser.extractArguments("	  help 			contacts   show	 "));
		}
		
		@Test
		void test_extract_arguments_for_contacts_add() {
			Assertions.assertArrayEquals(new String[]{"Bob", "127.0.0.1", "80"}, CommandParser.extractArguments("contacts add Bob 127.0.0.1 80"));
			Assertions.assertArrayEquals(new String[]{"Alice", "168.0.0.8", "25565"}, CommandParser.extractArguments("contacts add Alice 168.0.0.8 25565"));
			
			Assertions.assertArrayEquals(new String[]{"Bob", "127.0.0.1", "80"}, CommandParser.extractArguments("	contacts	  add 	Bob 	 127.0.0.1	  80"));
			Assertions.assertArrayEquals(new String[]{"Alice", "168.0.0.8", "25565"}, CommandParser.extractArguments(" contacts 	 add 	Alice 	168.0.0.8 		25565"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_remove() {
			Assertions.assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("contacts remove Alice"));
			Assertions.assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("contacts remove Bob"));
			
			Assertions.assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("	 contacts 	  remove	 Alice  	"));
			Assertions.assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments(" 	contacts 	remove 	    	Bob	 	"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_update() {
			Assertions.assertArrayEquals(new String[]{"Alice", "ip", "168.0.0.4"}, CommandParser.extractArguments("contacts update Alice ip 168.0.0.4"));
			Assertions.assertArrayEquals(new String[]{"Alice", "name", "Bob"}, CommandParser.extractArguments("contacts update Alice name Bob"));
			Assertions.assertArrayEquals(new String[]{"Cassie", "port", "3434"}, CommandParser.extractArguments("contacts update Cassie port 3434"));
			
			Assertions.assertArrayEquals(new String[]{"Alice", "ip", "168.0.0.4"}, CommandParser.extractArguments("  contacts 	 update 	 Alice	 ip		 168.0.0.4 "));
			Assertions.assertArrayEquals(new String[]{"Alice", "name", "Bob"}, CommandParser.extractArguments("		contacts		 update 	 Alice 	  name	 Bob "));
			Assertions.assertArrayEquals(new String[]{"Cassie", "port", "3434"}, CommandParser.extractArguments("	 contacts 	 update 	Cassie 	 port	 3434	 "));
		}
		
		@Test
		void test_extract_arguments_for_contacts_search() {
			Assertions.assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("contacts search Alice"));
			Assertions.assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("contacts search Bob"));
			
			Assertions.assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("	 contacts 	 search 		Alice		"));
			Assertions.assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("		contacts 		search 	 Bob		"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_show() {
			Assertions.assertArrayEquals(new String[]{}, CommandParser.extractArguments("contacts show"));
		}
	}
	
	
}
