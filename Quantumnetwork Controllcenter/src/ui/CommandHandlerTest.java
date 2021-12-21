package ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

	@Test
	void Null_Input_Throws_Exception() {
		IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> {CommandHandler.processCommand(null);});
		assertEquals(thrown.getMessage(), CommandHandler.NULL_COMMAND); // Correct error message is given
	}
	
	@Test
	void Empty_Input_Is_Invalid_Command() {
		assertEquals(CommandHandler.INVALID_COMMAND, CommandHandler.processCommand(""));
	}
	
	@Test
	void Garbage_Input_Is_Invalid_Command() {
		assertEquals(CommandHandler.INVALID_COMMAND, CommandHandler.processCommand("jj jj m mm m 3 333 1 xyz"));
	}
	
	// Temporary
	@Test
	void Help_Gives_Correct_Output() {
		assertEquals("There are no commands available at this moment in developement.", CommandHandler.processCommand("help"));
	}
	
	/*
	 * TODO: Tests for help [command]
	 */
	
}
