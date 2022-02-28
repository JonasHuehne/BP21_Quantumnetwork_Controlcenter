package ui;

import static ui.CommandConstants.*;

import java.util.ArrayList;

import networkConnection.ConnectionState;

/**
 * This enum contains the list of available console commands.
 * Each command has three attributes:
 *  A regular expression describing its syntax {@link #commandSyntax}.
 *  A help text describing its function and syntax {@link #helpText}.
 *  
 * The name of a command is equal to the name of the enum element that identifies it,
 * although in lower case and with underscores replaced by spaces. That means that:
 * {@link #CONTACTS_SHOW} would become "contacts show".
 * @author Sasha Petri
 */
public enum Command {

	// TODO: Potentially remove this later
	// Helpful resources on regular expressions:
	// https://docs.oracle.com/javase/tutorial/essential/regex/
	// https://www.vogella.com/tutorials/JavaRegularExpressions/article.html
	// https://regex101.com/
	
	/*
	 * For the moment, the only syntax requirements enforced on names by the parser is: no whitespaces.
	 * And the only syntax requirement enforced on IPs is that they consist of only "." and numbers (IPv4).
	 * Semantic requirements for IPs (i.e. 256.0.555.1234 being invalid) are not enforced here.
	 * Other naming restrictions such as Umlaute not being allowed is also not enforced here.
	 * Enforcing these falls under the responsibility of the CommunicationList into which names & IPs are inserted.
	 */
	
	HELP     
		("help", PATTERN_HELP, HR_ARGS_HELP, HELP_SHORT_HELP, HELP_LONG_HELP, Category.GENERAL),
 	
	CONTACTS_SHOW   
 		("contacts show", PATTERN_CONTACTS_SHOW, HR_ARGS_CONTACTS_SHOW, 
 		HELP_SHORT_CONTACTS_SHOW, HELP_LONG_CONTACTS_SHOW, Category.COMMUNICATION_LIST),
 	CONTACTS_SEARCH   
 		("contacts search", PATTERN_CONTACTS_SEARCH, HR_ARGS_CONTACTS_SEARCH, 
 		HELP_SHORT_CONTACTS_SEARCH, HELP_LONG_CONTACTS_SEARCH, Category.COMMUNICATION_LIST),
 	CONTACTS_REMOVE   
 		("contacts remove", PATTERN_CONTACTS_REMOVE, HR_ARGS_CONTACTS_REMOVE, 
 		HELP_SHORT_CONTACTS_REMOVE, HELP_LONG_CONTACTS_REMOVE, Category.COMMUNICATION_LIST),
 	CONTACTS_ADD   
 		("contacts add", PATTERN_CONTACTS_ADD, HR_ARGS_CONTACTS_ADD, 
 		HELP_SHORT_CONTACTS_ADD, HELP_LONG_CONTACTS_ADD, Category.COMMUNICATION_LIST),
 	CONTACTS_UPDATE   
 		("contacts update", PATTERN_CONTACTS_UPDATE, HR_ARGS_CONTACTS_UPDATE, 
 		HELP_SHORT_CONTACTS_UPDATE, HELP_LONG_CONTACTS_UPDATE, Category.COMMUNICATION_LIST),
 	CONTACTS_SHOWPK   
 		("contacts showpk", PATTERN_CONTACTS_SHOWPK, HR_ARGS_CONTACTS_SHOWPK, 
 		HELP_SHORT_CONTACTS_SHOWPK, HELP_LONG_CONTACTS_SHOWPK, Category.COMMUNICATION_LIST),
 	
 	CONNECTIONS_ADD   
 		("connections add", PATTERN_CONNECTIONS_ADD, HR_ARGS_CONNECTIONS_ADD, 
 		HELP_SHORT_CONNECTIONS_ADD, HELP_LONG_CONNECTIONS_ADD, Category.CONNECTIONS),
 	CONNECTIONS_REMOVE  
 		("connections remove", PATTERN_CONNECTIONS_REMOVE, HR_ARGS_CONNECTIONS_REMOVE, 
 		HELP_SHORT_CONNECTIONS_REMOVE, HELP_LONG_CONNECTIONS_REMOVE, Category.CONNECTIONS),
 	CONNECTIONS_SHOW  
 		("connections show", PATTERN_CONNECTIONS_SHOW, HR_ARGS_CONNECTIONS_SHOW, 
 		HELP_SHORT_CONNECTIONS_SHOW, HELP_LONG_CONNECTIONS_SHOW, Category.CONNECTIONS),
 	CONNECTIONS_CLOSE  
 		("connections close", PATTERN_CONNECTIONS_CLOSE, HR_ARGS_CONNECTIONS_CLOSE, 
 		HELP_SHORT_CONNECTIONS_CLOSE, HELP_LONG_CONNECTIONS_CLOSE, Category.CONNECTIONS),
 	CONNECT_TO    
 		("connect to", PATTERN_CONNECTIONS_CONNECT_TO, HR_ARGS_CONNECTIONS_CONNECT_TO, 
 		HELP_SHORT_CONNECTIONS_CONNECT_TO, HELP_LONG_CONNECTIONS_CONNECT_TO, Category.CONNECTIONS),
 	WAIT_FOR    
 		("wait for", PATTERN_CONNECTIONS_WAIT_FOR, HR_ARGS_CONNECTIONS_WAIT_FOR, 
 		HELP_SHORT_CONNECTIONS_WAIT_FOR, HELP_LONG_CONNECTIONS_WAIT_FOR, Category.CONNECTIONS),
 	HELLO_WORLD    
 		("hello world", PATTERN_CONNECTIONS_HELLO_WORLD, HR_ARGS_CONNECTIONS_HELLO_WORLD, 
 		HELP_SHORT_CONNECTIONS_HELLO_WORLD, HELP_LONG_CONNECTIONS_HELLO_WORLD, Category.CONNECTIONS),
 	
 	DEBUG_GENSIGPAIR  
 		("gensigs", PATTERN_DEBUG_GENSIGPAIR, HR_ARGS_DEBUG_GENSIGPAIR, 
 		HELP_SHORT_DEBUG_GENSIGPAIR, HELP_LONG_DEBUG_GENSIGPAIR, Category.DEBUG),
 	DEBUG_SETPK    
 		("setpk", PATTERN_DEBUG_SETPK, HR_ARGS_DEBUG_SETPK, 
 		HELP_SHORT_DEBUG_SETPK, HELP_LONG_DEBUG_SETPK, Category.DEBUG),
 	DEBUG_SETUP_LOCAL_CONS 
 		("setup local cons", PATTERN_DEBUG_SETUP_LOCAL_CONS, HR_ARGS_DEBUG_SETUP_LOCAL_CONS, 
 		HELP_SHORT_DEBUG_SETUP_LOCAL_CONS, HELP_LONG_DEBUG_SETUP_LOCAL_CONS, Category.DEBUG),
 	DEBUG_CLEAR_COMMLIST 
 		("clear commlist", PATTERN_DEBUG_CLEAR_COMMLIST, HR_ARGS_DEBUG_CLEAR_COMMLIST, 
 		HELP_SHORT_DEBUG_CLEAR_COMMLIST, HELP_LONG_DEBUG_CLEAR_COMMLIST, Category.DEBUG),
 	DEBUG_SPAM    
 		("spam", PATTERN_DEBUG_SPAM, HR_ARGS_DEBUG_SPAM, 
 		HELP_SHORT_DEBUG_SPAM, HELP_LONG_DEBUG_SPAM, Category.DEBUG);
	
	/** Unique name of the command, in lower case */
	private final String commandName;
	/** Regex pattern of the command, includes the command name and syntax of the arguments. 
	 * Command name is not case sensitive, arguments may or may not be depending on the exact command. */
	private final String commandPattern;
	/** The help text of a command describes its proper syntax in natural language, as well as its function. */
	private final String helpText;
	/** Human readable arguments of the Command. Displayed in the console when "help commandName" is entered. */
	private String hr_arguments;
	/** Short help text for the Command. Displayed next to the command name when "help" is entered in the console.*/
	private String helpText_short;
	/** Category of the Command. Used for grouping. */
	private Category category;
	
	/**
	 * Constructor.
	 * @param commandName
	 * 		Name of the command, to be entered in the console <br>
	 * 		Must be unique for each member of this enum!
	 * @param argumentsPattern
	 * 		Some Commands, such as {@link #CONTACTS_ADD} allow or require more than just the command name to be entered. <br>
	 * 		This parameter describes the syntax of the arguments following the command name, e.g. " (\\.+)?" for the help command
	 * 		because it may be optionally followed by any number of characters (given at least one whitespace distance from "help").
	 * @param argumentsHumanReadable
	 * 		A human-readable description of the arguments associated with this command. <br>
	 * 		For example, the command {@link #CONTACTS_ADD} may have "\<name\> \<ip\> \<port\>" as human readable arguments,
	 * 		informing the reader that it will add a contact with the specified name, ip and port.
	 * @param helpShort
	 * 		A short help text, describing what the command does.
	 * @param helpLong
	 * 		A long help text, describing what the command does in detail.
	 * @param category
	 * 		The category of the command, used for grouping.
	 */
	Command (String commandName, String argumentsPattern, String argumentsHumanReadable, String helpShort, String helpLong, Category category) {
		this.commandName 	= commandName;
		this.commandPattern = "(?i)" + commandName + "(?-i)" + argumentsPattern;
		this.hr_arguments	= argumentsHumanReadable;
		this.helpText_short = helpShort;
		this.helpText		= helpLong;
		this.category		= category;
	}
	
	/* Other fields or constructor args that could be potentially added in the future would be:
	 *  - Aliases (e.g. "cl add" for "contacts add"), could be handled as an array or via varags
	 *    and then integrated into the commandSyntax regex
	 *  - Error help text (possibly as a map of Exception names to Strings?) -> maybe as a different class */
	
	/**
	 * @return The name of the command, as entered in the console.
	 */
	public String getCommandName() {
		return commandName;
	}
	
	/**
	 * Intended to be used for debug / testing purposes, for pattern matching use {@link CommandParser#match(String)} if possible.
	 * @return The regular expression describing this commands accepted syntax.
	 */
	public String getCommandPattern() {
		return commandPattern;
	}
	
	/**
	 * @return A long text describing the function of this command.
	 * Does not include the syntax, for this use {@link #getHumanReadableSyntax()},
	 * but may contain additional info on what certain arguments of the command mean.
	 */
	public String getHelp() {
		return this.helpText;
	}
	
	/**
	 * @return A short text describing the function of this command.
	 */
	public String getShortHelp() {
		return this.helpText_short;
	}
	
	/**
	 * @return The Category of this Command.
	 */
	public Category getCategory() {
		return this.category;
	}
	
	/**
	 * @return A human readable description of this Command's syntax, i.e. which arguments are expected.
	 * Will not include the arguments name.
	 */
	public String getHumanReadableSyntax() {
		return hr_arguments;
	}
	
	/**
	 * Returns all Commands starting with the given prefix.
	 * @param prefix
	 * 		a string that is either a full command name, or the start of one or more command names <br>
	 * 		(e.g. "help", "contacts add" would be a full name, "contacts" would be the start of multiple commands)
	 * @return
	 * 		all Commands with a command name that starts with <code>prefix<code>
	 */
	public static ArrayList<Command> suggestMatchingCommands(String prefix) {
		ArrayList<Command> matchingCommands = new ArrayList<Command>();
		for (Command c : Command.values()) {
			if (c.getCommandName().startsWith(prefix)) {
				matchingCommands.add(c);
			}
		}
		return matchingCommands;
	}
}



