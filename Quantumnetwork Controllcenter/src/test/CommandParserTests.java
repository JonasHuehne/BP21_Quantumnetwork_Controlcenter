import static org.junit.jupiter.api.Assertions.*;

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
			String commandNameWithRandomWhitespaces = HelperMethods.randomizeWhiteSpace(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(commandNameWithRandomWhitespaces, true));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = false <br>
		 * With varying whitespaces in front and in the middle of the command
		 */
		void test_GetCommandOfName_varying_whitespaces_nonstrict(Command c) {
			String commandNameWithRandomWhitespaces = HelperMethods.randomizeWhiteSpace(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(commandNameWithRandomWhitespaces, false));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = true <br>
		 * With the command name not being typed solely in lower case
		 */
		void test_GetCommandOfName_case_sensitivity(Command c) {
			String randomCapitalizedCommandName = HelperMethods.randomizeCapitalization(c.getCommandName());
			assertEquals(c, CommandParser.getCommandOfName(randomCapitalizedCommandName, true));
		}
		
		@ParameterizedTest
		@EnumSource(Command.class)
		/**
		 * Test whether {@link CommandParser#getCommandOfName(String, boolean)} works if only the command name is entered, and strict = false <br>
		 * With the command name not being typed solely in lower case
		 */
		void test_GetCommandOfName_case_sensitivity_nonstrict(Command c) {
			String randomCapitalizedCommandName = HelperMethods.randomizeCapitalization(c.getCommandName());
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
			assertEquals(Command.CONTACTS_ADD, CommandParser.getCommandOfName(contacts_add + " Jimmy", false));
			assertEquals(Command.CONTACTS_SHOW, CommandParser.getCommandOfName(contacts_show + " Annie", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName(contacts_update + " Annie ip", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName(contacts_update + " Cassie height 172cm", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName(contacts_update + " Joanne pk", false));
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.getCommandOfName(contacts_update + " Joanne pk delete", false));
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
			
			assertEquals(Command.CONTACTS_ADD, CommandParser.match(contacts_add + " Bob 127.0.0.1 80"));
			assertEquals(
					Command.CONTACTS_ADD, 
					CommandParser.match(HelperMethods.randomizeWhiteSpace(contacts_add + " Bob 127.0.0.1 80"))
			);
			
			assertEquals(Command.CONTACTS_UPDATE, CommandParser.match(contacts_update + " Bob   name  Bobbie "));
			assertEquals(
					Command.CONTACTS_UPDATE, 
					CommandParser.match(HelperMethods.randomizeWhiteSpace(contacts_update + " Bob  name   Bobbie "))
			);
		
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
			assertNull(CommandParser.match(contacts_add + " Bob"));
			assertNull(CommandParser.match(contacts_add + " Bob 127.0.0.1"));
			assertNull(CommandParser.match(contacts_add + " Bob 127.0.0.1 "));
			assertNull(CommandParser.match(contacts_add + " Bob 127.0.0.1 x"));
			
			assertNull(CommandParser.match(contacts_remove));
			assertNull(CommandParser.match(contacts_remove + " "));
			assertNull(CommandParser.match(contacts_remove + " 	 ")); 
			assertNull(CommandParser.match(contacts_remove + " Alice Bob"));
			
			assertNull(CommandParser.match(contacts_show + " all"));
			
			assertNull(CommandParser.match(contacts_update));
			assertNull(CommandParser.match(contacts_update + " "));
			assertNull(CommandParser.match(contacts_update + " name"));
			assertNull(CommandParser.match(contacts_update + " Bob name Alice ip 127.0.0.1"));
			assertNull(CommandParser.match(contacts_update + " Alice ip "));
			assertNull(CommandParser.match(contacts_update + " Alice ip PotatoChips"));
			assertNull(CommandParser.match(contacts_update + " Cassie port Potato"));
			assertNull(CommandParser.match(contacts_update + " Alicia pk ABC"));
			assertNull(CommandParser.match(contacts_update + " Bob Bobbington name Alice Aliceton"));
			assertNull(CommandParser.match(contacts_update + " pk"));
			assertNull(CommandParser.match(contacts_update + " pk \"???\""));
			
			assertNull(CommandParser.match("     "));
		}
		
		@Test
		void test_match_for_connection_commands_entered_correctly() {
			assertEquals(Command.CONNECTIONS_ADD, CommandParser.match(connections_add + " Alice"));
			assertEquals(Command.CONNECTIONS_ADD, CommandParser.match(connections_add + " Alice 12345"));
			
			assertEquals(Command.CONNECTIONS_REMOVE, CommandParser.match(connections_remove + " Alice"));
			
			assertEquals(Command.CONNECTIONS_SHOW, CommandParser.match(connections_show));
			
			assertEquals(Command.CONNECT_TO, CommandParser.match(connect_to + " Alice"));
			
			assertEquals(Command.WAIT_FOR, CommandParser.match(wait_for + " Alice"));
			
			assertEquals(Command.CONNECTIONS_CLOSE, CommandParser.match(connections_close + " Alice"));
			
			assertEquals(Command.HELLO_WORLD, CommandParser.match(hello_world + " Alice"));
		}
		
		@Test
		void test_match_for_connection_commands_entered_incorrectly() {
			assertNull(CommandParser.match(connections_add));
			assertNull(CommandParser.match(connections_add + " James Bond"));
			assertNull(CommandParser.match(connections_add + " Alice 1234 5678"));
			assertNull(CommandParser.match(connections_add + " Alice 9999999"));
			
			assertNull(CommandParser.match(connect_to));
			assertNull(CommandParser.match(connect_to + " 127.0.0.1 8381"));
			
			assertNull(CommandParser.match(connections_close));
			assertNull(CommandParser.match(connections_close + " Alice Bob"));
			assertNull(CommandParser.match(connections_close + " 127.0.0.1 17441"));;
			
			assertNull(CommandParser.match(connections_remove));
			assertNull(CommandParser.match(connections_remove + " Alice Bob"));
			assertNull(CommandParser.match(connections_remove + " 127.0.0.1 17441"));
			
			assertNull(CommandParser.match(connections_show + " all"));
			assertNull(CommandParser.match(connections_show + " Alice"));
			assertNull(CommandParser.match(connections_show + " 127.0.0.1"));
			
			assertNull(CommandParser.match(wait_for));
			assertNull(CommandParser.match(wait_for + " Alice, Bob"));
			
			assertNull(CommandParser.match(hello_world));
			assertNull(CommandParser.match(hello_world + " Alice, Bob, Cassie, Jim"));
		}
	}
	
	@Nested
	class Test_Extract_Arguments {
		
		/**
		 * Helper Method to reduce code duplication.
		 * For a given command (with arguments) it asserts that running
		 * extractArguments on the command returns the expected arguments, and
		 * assert that varying the whitespaces in the command (e.g. "help commandName" -> " help   commandName  ")
		 * makes no difference.
		 * @param expectedArguments
		 * 		the expected arguments to be extracted from the command, e.g. ["Bob", "127.0.0.1", "80"]
		 * @param commandInput
		 * 		a command with arguments such as "contacts add Bob 127.0.0.1 80"
		 */
		void assertExtractedArgumentsAre(String[] expectedArguments, String commandInput) {
			assertArrayEquals(expectedArguments, CommandParser.extractArguments(commandInput));
			
			String commandWithVaryingWhitespace = HelperMethods.randomizeWhiteSpace("  " + commandInput + "  ");
			assertArrayEquals(expectedArguments, CommandParser.extractArguments(commandWithVaryingWhitespace));
		}
		
		@Test
		/**
		 * Asserts that extract arguments returns null for invalid commands / invalid syntax or null inputs.
		 */
		void test_extract_arguments_for_invalid_commands() {
			assertNull(CommandParser.extractArguments(null));
			assertNull(CommandParser.extractArguments("    "));
			assertNull(CommandParser.extractArguments("34rgjdrgjergjer"));
			assertNull(CommandParser.extractArguments(contacts_remove + " "));
			assertNull(CommandParser.extractArguments(contacts_update + " Cassie port Potato"));
		}
		
		@Test
		void test_extract_arguments_for_help() {
			
			assertArrayEquals(new String[]{}, CommandParser.extractArguments(help));
			assertArrayEquals(new String[]{}, CommandParser.extractArguments(help + " "));
			assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments(help + " " + help));
			assertArrayEquals(new String[]{"help"}, CommandParser.extractArguments("   " + help + "     " + help + "   "));
			
			/* 
			 * Help is allowed to be followed by any String syntactically. 
			 * The semantics of help only giving a valid output if followed by nothing or an actual command are not the job of the CommandParser. 
			 */
			
			assertArrayEquals(new String[]{"contacts", "add"}, 		CommandParser.extractArguments(help + " contacts add"));
			assertArrayEquals(new String[]{"contacts", "remove"}, 	CommandParser.extractArguments(help + " contacts remove"));
			assertArrayEquals(new String[]{"contacts", "show"}, 	CommandParser.extractArguments(help + " contacts show"));
			
			assertArrayEquals(new String[]{"contacts", "add"}, 		CommandParser.extractArguments(help + " 	 contacts 	add		"));
			assertArrayEquals(new String[]{"contacts", "remove"}, 	CommandParser.extractArguments(help + "  		contacts 	remove   "));
			assertArrayEquals(new String[]{"contacts", "show"}, 	CommandParser.extractArguments(help + " 			contacts   show	 "));
		}
		
		@Nested
		class Test_Extract_Arguments_CommList_Commands {
			@Test
			void test_extract_arguments_for_contacts_add() {		
				assertExtractedArgumentsAre(new String[]{"Bob", "127.0.0.1", "80"}, 		contacts_add + " Bob 127.0.0.1 80");
				assertExtractedArgumentsAre(new String[]{"Alice", "168.0.0.8", "25565"}, 	contacts_add + " Alice 168.0.0.8 25565");
			}
			
			@Test
			void test_extract_arguments_for_contacts_remove() {
				assertExtractedArgumentsAre(new String[]{"Alice"}, 	contacts_remove + " Alice");
				assertExtractedArgumentsAre(new String[]{"Bob"}, 	contacts_remove + " Bob");
			}
			
			@Test
			void test_extract_arguments_for_contacts_update() {
				assertExtractedArgumentsAre(new String[]{"Alice", "ip", "168.0.0.4"}, 	contacts_update + " Alice ip 168.0.0.4");
				assertExtractedArgumentsAre(new String[]{"Alice", "name", "Bob"}, 		contacts_update + " Alice name Bob");
				assertExtractedArgumentsAre(new String[]{"Cassie", "port", "3434"}, 	contacts_update + " Cassie port 3434");		
			}
			
			@Test
			void test_extract_arguments_for_contacts_update_pk() {
				assertExtractedArgumentsAre(new String[] {"Alicia", "pk", "\"pkForTesting_1\""}, 		contacts_update + " Alicia pk \"pkForTesting_1\"");
				assertExtractedArgumentsAre(new String[] {"Alicia", "pk", "\"nonsense_xyz.png.mp4\""}, 	contacts_update + " Alicia pk \"nonsense_xyz.png.mp4\"");	
				assertExtractedArgumentsAre(new String[] {"Alicia", "pk", "remove"}, 					contacts_update + " Alicia pk remove");
			}
			
			@Test
			void test_extract_arguments_for_contacts_search() {
				assertExtractedArgumentsAre(new String[]{"Alice"}, 	contacts_search + " Alice");
				assertExtractedArgumentsAre(new String[]{"Bob"}, 	contacts_search + " Bob");
			}
			
			@Test
			void test_extract_arguments_for_contacts_show() {
				assertExtractedArgumentsAre(new String[]{}, contacts_show);
			}
			
			@Test
			void test_extract_arguments_for_contacts_showpk() {
				assertExtractedArgumentsAre(new String[] {"Alexa"}, contacts_showpk + " Alexa");
				assertExtractedArgumentsAre(new String[] {"Bobbie"}, contacts_showpk + " Bobbie");
			}

		}
		
		@Nested
		class Test_Extract_Arguments_Connection_Commands {
			
			@Test
			void test_extract_arguments_connections_add() {
				assertExtractedArgumentsAre(new String[] {"Bob"}, connections_add + " Bob");
				assertExtractedArgumentsAre(new String[] {"Bob", "17171"}, connections_add + " Bob 17171");
				assertExtractedArgumentsAre(new String[] {"Alice"}, connections_add + " Alice");
				assertExtractedArgumentsAre(new String[] {"Alice", "4444"}, connections_add + " Alice 4444");
			}
			
			@Test
			void test_extract_arguments_connections_remove() {
				assertExtractedArgumentsAre(new String[] {"Alice"}, connections_remove + " Alice");	
				assertExtractedArgumentsAre(new String[] {"Bob"}, connections_remove + " Bob");		
			}
			
			@Test
			void test_extract_arguments_connections_show() {
				assertExtractedArgumentsAre(new String[] {}, connections_show);						
			}
			
			@Test
			void test_extract_arguments_connect_to() {
				assertExtractedArgumentsAre(new String[] {"Alice"}, connect_to + " Alice");			
				assertExtractedArgumentsAre(new String[] {"Bob"}, connect_to + " Bob");			
			}
			
			@Test
			void test_extract_arguments_wait_for() {
				assertExtractedArgumentsAre(new String[] {"Alice"}, wait_for + " Alice");			
				assertExtractedArgumentsAre(new String[] {"Bob"}, wait_for + " Bob");							
			}
			
			@Test
			void test_extract_arguments_connections_close() {
				assertExtractedArgumentsAre(new String[] {"Marius"}, wait_for + " Marius");
				assertExtractedArgumentsAre(new String[] {"Eve"}, wait_for + " Eve");
			}
			
			@Test
			void test_extract_arguments_hello_world() {
				assertExtractedArgumentsAre(new String[] {"Alex"}, hello_world + " Alex");		
				assertExtractedArgumentsAre(new String[] {"Steve"}, hello_world + " Steve");				
			}
		}
		
	}
	
	
}
