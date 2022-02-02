import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ui.Command;
import ui.CommandParser;

/**
 * @author Sasha Petri
 */
class CommandParserTests {
	
	/**
	 * Used to randomize the whitespace in a string.
	 * @param input
	 * 		any string
	 * @return
	 * 		the same string, but each whitespace will be replaced by 1 to 5 whitespaces (this means that through randomness, there may be no change some times)
	 */
	private String helper_randomizeWhiteSpace(String input) {
		char[] inputArray = input.toCharArray();
		String output = "";
		Random r = new Random();
		for (char c : inputArray) {
			if(Character.isWhitespace(c)) {
				int randomWhiteSpaceAmount = r.nextInt(5) + 1; // random number between 1 and 5
				output += " ".repeat(randomWhiteSpaceAmount);
			} else {
				output += c;
			}
		}
		return output;
	}
	
	private String helper_randomizeCapitalization(String input) {
		char[] inputArray = input.toCharArray();
		String output = "";
		Random r = new Random();
		for (char c : inputArray) {
			if(Character.isAlphabetic(c)) {
				boolean uppercase = r.nextBoolean();
				if(uppercase) {
					output += Character.toUpperCase(c);
				} else {
					output += Character.toLowerCase(c);
				}
			} else {
				output += c;
			}
		}
		return output;
	}
	
	@Test
	void test_norm_input() {
		assertNull(CommandParser.normInput(null));
		assertEquals("abc", CommandParser.normInput("       abc     		"));
		assertEquals("a b c", CommandParser.normInput(" a	b		 c	 "));
	}
	
	@Nested
	class Test_GetCommandOfName {

		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = true
		 */
		void test_GetCommandOfName_normal(Command c) {
			assertEquals(c, CommandParser.getCommandOfName(c.getCommandName(), true));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = false
		 */
		void test_GetCommandOfName_normal_nonstrict(Command c) {
			assertEquals(c, CommandParser.getCommandOfName(c.getCommandName(), false));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = true <br>
		 * With varying whitespaces in front and in the middle of the command
		 */
		void test_GetCommandOfName_varying_whitespaces(Command c) {
			String commandNameWithRandomWhitespaces = helper_randomizeWhiteSpace(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(commandNameWithRandomWhitespaces, true));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = false <br>
		 * With varying whitespaces in front and in the middle of the command
		 */
		void test_GetCommandOfName_varying_whitespaces_nonstrict(Command c) {
			String commandNameWithRandomWhitespaces = helper_randomizeWhiteSpace(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(commandNameWithRandomWhitespaces, false));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = true <br>
		 * With the command name not being typed solely in lower case
		 */
		void test_GetCommandOfName_case_sensitivity(Command c) {
			String randomCapitalizedCommandName = helper_randomizeCapitalization(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(randomCapitalizedCommandName, true));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = false <br>
		 * With the command name not being typed solely in lower case
		 */
		void test_GetCommandOfName_case_sensitivity_nonstrict(Command c) {
			String randomCapitalizedCommandName = helper_randomizeCapitalization(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(randomCapitalizedCommandName, false));
		}
		
		@Test
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works (returns null) if null is entered, and strict = true <br>
		 */
		void test_GetCommandOfName_null() {
			assertNull(CommandParser.getCommandOfName(null, true));
			assertNull(CommandParser.getCommandOfName("dfvndfn34534vnnvn11fnnfnnaaaa", true));
		}
		
		@Test
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works (returns null) if null is entered, and strict = true <br>
		 */
		void test_GetCommandOfName_null_nonstrict() {
			assertNull(CommandParser.getCommandOfName(null, false));
			assertNull(CommandParser.getCommandOfName("dfvndfn34534vnnvn11fnnfnnaaaa", false));
		}
		
		@Test
		/**
		 * Test whether the nonstrict mode of {@link CommandParser#getCommandOfName(String, boolean)} works as intended <br>
		 */
		void test_GetCommandOfName_nonstrict_wrong_syntax()  {
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName("contacts add Jimmy", false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName("contacts show Annie", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Annie ip", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Cassie height 172cm", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Joanne pk", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName("contacts update Joanne pk delete", false));
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
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Alicia pk \"testPk_1\""));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match("contacts update Alicia pk remove"));
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
			assertNull(CommandParser.match("contacts update Bob name Alice ip 127.0.0.1"));
			assertNull(CommandParser.match("contacts update Alice ip "));
			assertNull(CommandParser.match("contacts update Alice ip PotatoChips"));
			assertNull(CommandParser.match("contacts update Cassie port Potato"));
			assertNull(CommandParser.match("contacts update Alicia pk ABC"));
			assertNull(CommandParser.match("contacts update Bob Bobbington name Alice Aliceton"));
			assertNull(CommandParser.match("contacts update pk"));
			assertNull(CommandParser.match("contacts update pk \"???\""));
			
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
			
			assertArrayEquals(new String[]{}, CommandParser.extractArguments("help"));
			assertArrayEquals(new String[]{}, CommandParser.extractArguments("help "));
			assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments("help help"));
			assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments("help 		help 	"));
			
			/* 
			 * Help is allowed to be followed by any String syntactically. 
			 * The semantics of help only giving a valid output if followed by nothing or an actual command are not the job of the CommandParser. 
			 */
			
			assertArrayEquals(new String[]{"contacts", "add"}, CommandParser.extractArguments("help contacts add"));
			assertArrayEquals(new String[]{"contacts", "remove"}, CommandParser.extractArguments("help contacts remove"));
			assertArrayEquals(new String[]{"contacts", "show"}, CommandParser.extractArguments("help contacts show"));
			
			assertArrayEquals(new String[]{"contacts", "add"}, CommandParser.extractArguments("		help 	 contacts 	add		"));
			assertArrayEquals(new String[]{"contacts", "remove"}, CommandParser.extractArguments("     help  		contacts 	remove   "));
			assertArrayEquals(new String[]{"contacts", "show"}, CommandParser.extractArguments("	  help 			contacts   show	 "));
		}
		
		@Test
		void test_extract_arguments_for_contacts_add() {
			assertArrayEquals(new String[]{"Bob", "127.0.0.1", "80"}, CommandParser.extractArguments("contacts add Bob 127.0.0.1 80"));
			assertArrayEquals(new String[]{"Alice", "168.0.0.8", "25565"}, CommandParser.extractArguments("contacts add Alice 168.0.0.8 25565"));
			
			assertArrayEquals(new String[]{"Bob", "127.0.0.1", "80"}, CommandParser.extractArguments("	contacts	  add 	Bob 	 127.0.0.1	  80"));
			assertArrayEquals(new String[]{"Alice", "168.0.0.8", "25565"}, CommandParser.extractArguments(" contacts 	 add 	Alice 	168.0.0.8 		25565"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_remove() {
			assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("contacts remove Alice"));
			assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("contacts remove Bob"));
			
			assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("	 contacts 	  remove	 Alice  	"));
			assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments(" 	contacts 	remove 	    	Bob	 	"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_update() {
			assertArrayEquals(new String[]{"Alice", "ip", "168.0.0.4"}, CommandParser.extractArguments("contacts update Alice ip 168.0.0.4"));
			assertArrayEquals(new String[]{"Alice", "name", "Bob"}, CommandParser.extractArguments("contacts update Alice name Bob"));
			assertArrayEquals(new String[]{"Cassie", "port", "3434"}, CommandParser.extractArguments("contacts update Cassie port 3434"));
			
			assertArrayEquals(new String[]{"Alice", "ip", "168.0.0.4"}, CommandParser.extractArguments("  contacts 	 update 	 Alice	 ip		 168.0.0.4 "));
			assertArrayEquals(new String[]{"Alice", "name", "Bob"}, CommandParser.extractArguments("		contacts		 update 	 Alice 	  name	 Bob "));
			assertArrayEquals(new String[]{"Cassie", "port", "3434"}, CommandParser.extractArguments("	 contacts 	 update 	Cassie 	 port	 3434	 "));			
		}
		
		@Test
		void test_extract_arguments_for_contacts_update_pk() {
			assertArrayEquals(new String[] {"Alicia", "pk", "\"pkForTesting_1\""}, CommandParser.extractArguments("contacts update Alicia pk \"pkForTesting_1\""));
			assertArrayEquals(new String[] {"Alicia", "pk", "\"nonsense_xyz.png.mp4\""}, CommandParser.extractArguments("contacts update Alicia pk \"nonsense_xyz.png.mp4\""));	
			assertArrayEquals(new String[] {"Alicia", "pk", "remove"}, CommandParser.extractArguments("contacts update Alicia pk remove"));
		
			assertArrayEquals(new String[] {"Alicia", "pk", "\"pkForTesting_1\""}, CommandParser.extractArguments("	contacts  update   Alicia  pk   \"pkForTesting_1\" "));
			assertArrayEquals(new String[] {"Alicia", "pk", "\"nonsense_xyz.png.mp4\""}, CommandParser.extractArguments("  contacts update  Alicia pk   \"nonsense_xyz.png.mp4\"   "));	
			assertArrayEquals(new String[] {"Alicia", "pk", "remove"}, CommandParser.extractArguments(" contacts 	update Alicia pk   	remove "));
		}
		
		@Test
		void test_extract_arguments_for_contacts_search() {
			assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("contacts search Alice"));
			assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("contacts search Bob"));
			
			assertArrayEquals(new String[]{"Alice"}, CommandParser.extractArguments("	 contacts 	 search 		Alice		"));
			assertArrayEquals(new String[]{"Bob"}, CommandParser.extractArguments("		contacts 		search 	 Bob		"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_show() {
			assertArrayEquals(new String[]{}, CommandParser.extractArguments("contacts show"));
		}
		
		@Test
		void test_extract_arguments_for_contacts_showpk() {
			assertArrayEquals(new String[] {"Alexa"}, CommandParser.extractArguments("contacts showpk Alexa"));
			assertArrayEquals(new String[] {"Bobbie"}, CommandParser.extractArguments("contacts showpk Bobbie"));
			
			assertArrayEquals(new String[] {"Alexa"}, CommandParser.extractArguments(helper_randomizeWhiteSpace("contacts showpk Alexa")));
			assertArrayEquals(new String[] {"Bobbie"}, CommandParser.extractArguments(helper_randomizeWhiteSpace("contacts showpk Bobbie")));
		}
	}
	
	
}
