package ui;

/**
 * The purpose of this class is to process text commands (given as Strings) and execute corresponding program method.
 * @author Sasha Petri
 */
public class CommandHandler {

	private CommandHandler() {
		
	}
	
	/** String returned by {@link #processCommand(String)} if the given command is not recognized (e.g. because of a typo, or because the input was empty) */
	public static final String INVALID_COMMAND = "Unrecognized Command.";
	/** Message given with the IllegalArgumentException that is thrown if {@link #processCommand(String)} is called with String = null*/
	public static final String NULL_COMMAND = "ERROR, could not process the given command! null is not a recognized command.";
	
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
	 * @param command
	 * 		a text command, not case sensitive
	 * @return
	 * 		relevant information for the user about the execution of the command
	 * 		e.g. for the command [connect-to ...] this could be "Successfully established Connection to [IP]. [...]", or "Error XYZ: Could not establish Connection because of [...]".
	 */
	public static String processCommand(String command) {
		
		/*
		 *  We could just return INVALID_COMMAND here, however, if the input is null, 
		 *  something likely went wrong enough to warrant an exception, whereas INVALID_COMMAND
		 *  is intended to simply inform the user that their input is not recognized as a command.
		 */
		if(command == null) throw new IllegalArgumentException(NULL_COMMAND);
		
		command = command.toLowerCase();
		
		switch (command) {
		case "help": 
			return "There are no commands available at this moment in developement.";
		default: // TODO: Potentially add a "did you mean ... " function, or something similar
			return INVALID_COMMAND;
		}
		
	}
	
	
}
