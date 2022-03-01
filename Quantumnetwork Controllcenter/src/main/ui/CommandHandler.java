package ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import ui.CommandConstants.Category;

/**
 * The purpose of this class is to process text commands (given as Strings) and execute corresponding program method.
 * @author Sasha Petri
 */
public final class CommandHandler {

	private CommandHandler() {
		
	}
	
	/** Message given with the IllegalArgumentException that is thrown if {@link #processCommand(String)} is called with String = null*/
	public static final String NULL_COMMAND = "ERROR, could not process the given command (null). null is not a recognized command.";
	
	/*
	 * TODO: 
	 * This class will need to be continually expanded as the project becomes more complete, 
	 * with more commands being implemented to allow users to control the application via text.
	 */
	
	
	// Generally intended to be used in {@link ConsoleUI}, but is independent of that class and could for example just as well be used with sysin / sysout
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
				
			// Communication List Commands
			case CONTACTS_ADD:
				return CommunicationListCommandHandler.handleContactsAdd(commandArgs);
			case CONTACTS_REMOVE:
				return CommunicationListCommandHandler.handleContactsRemove(commandArgs[0]);
			case CONTACTS_SEARCH:
				return CommunicationListCommandHandler.handleContactsSearch(commandArgs[0]);
			case CONTACTS_SHOW:
				return CommunicationListCommandHandler.handleContactsShow();
			case CONTACTS_UPDATE:
				return CommunicationListCommandHandler.handleContactsUpdate(commandArgs);
			case CONTACTS_SHOWPK:
				return CommunicationListCommandHandler.handleShowPk(commandArgs[0]);
						
			// Connection Commands	
			case CONNECTIONS_ADD:
				return ConnectionCommandHandler.handleConnectionsAdd(commandArgs);
			case CONNECTIONS_SHOW:
				return ConnectionCommandHandler.handleConnectionShow();
			case CONNECT_TO:
				return ConnectionCommandHandler.handleConnectTo(commandArgs[0]);
			case WAIT_FOR:
				return ConnectionCommandHandler.handleWaitFor(commandArgs[0]);
			case HELLO_WORLD:
				return ConnectionCommandHandler.handleHelloWorld(commandArgs[0]);
			case CONNECTIONS_CLOSE:
				return ConnectionCommandHandler.handleConnectionsClose(commandArgs[0]);
			case CONNECTIONS_REMOVE:
				return ConnectionCommandHandler.handleConnectionsRemove(commandArgs[0]);
			
			// Debug Commands, not intended to be part of the final product, but useful for manual testing
			case DEBUG_GENSIGPAIR:
				return DebugCommandHandler.handleGenSigPair();
			case DEBUG_SETPK:
				return DebugCommandHandler.handleSetPk(commandArgs);
			case DEBUG_SETUP_LOCAL_CONS:
				return DebugCommandHandler.handleSetUpLocalConnections();
			case DEBUG_CLEAR_COMMLIST:
				return DebugCommandHandler.handleClearCommList();
			case DEBUG_SPAM:
				return DebugCommandHandler.handleSpam();
				
			default:
				return "ERROR - This Command is not implemented yet.";
		}
	}
	
	/**
	 * This method is used to handle the case of a user entering an invalid command. <p>
	 * A command can be invalid either because it is not recognized (textCommand does not correspond to a commandName of any {@link Command})
	 * or because the syntax of the command was wrong (e.g. "contacts update Bob port Potato").
	 * @param textCommand
	 * 		a command entered by a user, which can not be executed 
	 * @return
	 * 		a String informing the user that the command is unrecognized, if it does not correspond to any command in {@link Command} <p>
	 * 		or a String informing the user that the command could not be executed with the given arguments if the syntax was wrong
	 */
	private static String handleInvalidCommand(String textCommand) {
		// See if the user has entered a valid command (but with an unrecognized syntax)
		Command potentiallyValidCommand = CommandParser.getCommandOfName(textCommand, false);
		if (potentiallyValidCommand == null) {
			return "ERROR - UNRECOGNIZED COMMAND: " + textCommand + System.lineSeparator()
					+ "Please use \"help\" to see a list of recognized commands.";
		} else {
			return "ERROR - INVALID SYNTAX ON COMMAND: " + textCommand + System.lineSeparator()
					+ "Could not execute command \"" + potentiallyValidCommand.getCommandName() + "\" with the given arguments. " + System.lineSeparator()
					+ "Please use \"help " + potentiallyValidCommand.getCommandName() + "\" for information on the correct syntax.";
		}
	}
	
	/**
	 * Handles the execution of the help command.
	 * @param commandArgs
	 * 		the arguments with which the help command was called, may be empty
	 * @return
	 * 		if commandArgs is empty, returns a String containing a List of all available {@link Command}s <br>
	 * 		if commandArgs is not empty and instead contains a command name, 
	 * 		with or without arguments (e.g. ["contacts", "add"] or ["contacts", "add", "Bob"]),
	 * 		this method returns help for that command <br>
	 * 		if neither of these two cases applies, an error message is returned that no help can be provided, because commandArgs does not contain a valid command name
	 */
	private static String handleHelp(String[] commandArgs) {
		if(commandArgs.length == 0) { // User typed just "help"
			StringBuilder availableCommands = new StringBuilder();
		
			ArrayList<Command> allCommands = new ArrayList<Command>(Arrays.asList(Command.values()));
			
			// For each category
			for (Category cat : CommandConstants.Category.values()) {
				availableCommands.append(cat.getCategoryName() + ": "); // Display the name of the category
				availableCommands.append(System.lineSeparator());
				
				// Display each command with its name and short helptext
				ListIterator<Command> iterator = allCommands.listIterator();
				while(iterator.hasNext()) {
					Command nextCommand = iterator.next();
					if (nextCommand.getCategory() == cat) {
						availableCommands.append(String.format("  %-20s %s", nextCommand.getCommandName(), nextCommand.getShortHelp()));
						availableCommands.append(System.lineSeparator());
						iterator.remove();
					}
				}
				
				availableCommands.append(System.lineSeparator());
			}
			
			return availableCommands.toString();
			
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
				StringBuilder out = new StringBuilder();
				out.append("Syntax: " + matchedCommand.getCommandName() + " " + matchedCommand.getHumanReadableSyntax());
				out.append(System.lineSeparator());
				out.append(matchedCommand.getHelp());
				return out.toString();
			}
		} 
	}
	
	
	
}



