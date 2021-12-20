package ui;

/**
 * The purpose of this class is to process text commands (given as Strings) and execute corresponding program method.
 * @author Sasha Petri
 */
public class CommandHandler {

	private CommandHandler() {
		
	}
	
	
	// TODO: Make this JavaDoc good (for example, once we have actual functionality we can give real examples)
	// TODO: In the documentation, write down the available commands, their syntax, ...
	/* TODO: Actually implement any commands. For reasons of modularity & cohesion, do only as much as "neccessary" in this class, 
	 * 		 i.e. when a command is entered, this class should at most handle the parameters of the command and then call the appropriate method
	 * 		 e.g. KeyManager.deleteKeys(...) or whatever other method is appropriate to call for the entered command
	*/
	
	// Generally intended to be used in {@link ConsoleUI}, but is independend of that class and could for example just as well be used with sysin / sysout
	/**
	 * Processes a given text command and executes the corresponding program method.
	 * @param command
	 * 		a text command, not case sensitive
	 * @return
	 * 		relevant information for the user about the execution of the command
	 * 		e.g. for the command [connect-to ...] this could be "Successfully established Connection to [IP]. [...]", or "Error XYZ: Could not establish Connection because of [...]".
	 */
	public static String processCommand(String command) {
		
		switch (command) {
		case "help": 
			return "Sorry, at the moment there is no help available.";
		default:
			return "Unrecognized Command.";
		}
		
	}
	
}
