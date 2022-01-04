package ui;

import java.util.Arrays;

import CommunicationList.Database;
import CommunicationList.DbObject;

/**
 * The purpose of this class is to process text commands (given as Strings) and execute corresponding program method.
 * @author Sasha Petri
 */
public class CommandHandler {

	private CommandHandler() {
		
	}
	
	/** Message given with the IllegalArgumentException that is thrown if {@link #processCommand(String)} is called with String = null*/
	public static final String NULL_COMMAND = "ERROR, could not process the given command (null). null is not a recognized command.";
	
	/*
	 * TODO: 
	 * This class will need to be continually expanded as the project becomes more complete, 
	 * with more commands being implemented to allow users to control the application via text.
	 */
	
	/*
	 * TODO:
	 * Document each command externally (Syntax & Purpose) and also implement "help [command]" so the user can access additional info about the command.
	 * Could potentially use an Enum for this.
	 */
	
	// Generally intended to be used in {@link ConsoleUI}, but is independend of that class and could for example just as well be used with sysin / sysout
	/**
	 * Processes a given text command and executes the corresponding program method.
	 * @param textCommand
	 * 		a text command, not case sensitive, may not be null
	 * @return
	 * 		relevant information for the user about the execution of the command
	 * 		e.g. for the command [connect-to ...] this could be "Successfully established Connection to [IP]. [...]", or "Error XYZ: Could not establish Connection because of [...]".
	 * @throws IllegalArgumentException
	 * 		if the entered command is null
	 */
	public static String processCommand(String textCommand) throws IllegalArgumentException {
		
		/*
		 *  We could just return INVALID_COMMAND here, however, if the input is null, 
		 *  something likely went wrong enough to warrant an exception, whereas INVALID_COMMAND
		 *  is intended to simply inform the user that their input is not recognized as a command.
		 */
		if(textCommand == null) throw new IllegalArgumentException(NULL_COMMAND);
		
		Command command = CommandParser.match(textCommand);
		String[] commandArgs = CommandParser.extractArguments(textCommand);
			
		if(command == null) return handleInvalidCommand(textCommand);
		
		switch (command) {
			case HELP: 
				return handleHelp(commandArgs);
			case CONTACTS_ADD:
				return CommunicationListCommandHandler.handleContactsAdd(commandArgs);
			case CONTACTS_REMOVE:
				return CommunicationListCommandHandler.handleContactsRemove(commandArgs);
			case CONTACTS_SEARCH:
				return CommunicationListCommandHandler.handleContactsSearch(commandArgs);
			case CONTACTS_SHOW:
				return CommunicationListCommandHandler.handleContactsShow();
			case CONTACTS_UPDATE:
				return CommunicationListCommandHandler.handleContactsUpdate(commandArgs);
			default:
				return "Not implemented yet.";
		}
	}
	
	private static String handleInvalidCommand(String textCommand) {
		// See if the user has entered a valid command (but with an unrecognized syntax)
		Command potentiallyValidCommand = CommandParser.getCommandOfName(textCommand, false);
		System.out.println("Tried matching " + textCommand + " to a command, got " + potentiallyValidCommand);
		if (potentiallyValidCommand == null) {
			return "ERROR - UNRECOGNIZED COMMAND: " + textCommand + System.lineSeparator()
					+ "Please use \"help\" to see a list of recognized commands.";
		} else {
			return "ERROR - INVALID SYNTAX ON COMMAND: " + textCommand + System.lineSeparator()
					+ "Could not execute command \"" + potentiallyValidCommand.getCommandName() + "\" with the given arguments. " + System.lineSeparator()
					+ "Please use \"help " + potentiallyValidCommand.getCommandName() + "\" for information on the correct syntax.";
		}
	}
	
	private static String handleHelp(String[] commandArgs) {
		if(commandArgs.length == 0) { // User typed just "help"
			String availableCommands = "Available Commands: " + System.lineSeparator();
			for (Command c : Arrays.asList(Command.values())) { // TODO Potentially add a "short help text" for each Command, either in Command or a seperate enum
				availableCommands += " " + c.getCommandName() + System.lineSeparator();
			}
			return availableCommands;
		} else { // User typed "help [command]" - we don't yet know what command is, and if it is for example "contacts add" then commandArgs will be multiple args
			// from commandArgs, construct just one String (the String representing the command the user wants help with)
			String commandUserWantsHelpFor = "";
			for (String s : commandArgs) {
					commandUserWantsHelpFor += s + " ";
			}
			Command matchedCommand = CommandParser.getCommandOfName(commandUserWantsHelpFor, false);
			if(matchedCommand == null) {
				return commandUserWantsHelpFor + " is not a valid command, so no help can be provided for it.";
			} else {
				return matchedCommand.getHelp();
			}
		} 
	}
	
	
	
}



