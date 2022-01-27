package main.ui;

import java.io.File;

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
	
	HELP ("( .+)?", 
		"The help command displays a list of available commands. "
		 + "In addition it displays additional info on commands, by entering \"help <commandName>\". "
		 + "When describing the syntax of a command <argument> describes a mandatory argument, while [argument] describes an optional argument."),
	CONTACTS_SHOW ("", 
		"Displays all contacts in the communication list."
		+ System.lineSeparator() + System.lineSeparator()		 
		+ "Syntax: \"contacts show\""),
	CONTACTS_SEARCH (" \\S{1,255}", 
		"Searches for one entry in the communication list by name, displaying it if found. "
		 + "If no entry of that name is found, an error message is displayed. "
		+ System.lineSeparator() + System.lineSeparator()
		 + "Syntax: \"contacts search <name>\""),
	CONTACTS_REMOVE (" \\S{1,255}", 
		"Removes one entry in the communication list, given the name of the contact. "
		+ "If no entry of that name is found, an error message is displayed. "
		+ System.lineSeparator() + System.lineSeparator()
		+ "Syntax: \"contacts remove <name>\""),
	CONTACTS_ADD (" \\S{1,255} (\\d|\\.){1,255} \\d+", 
		"Adds one entry to the communication list, given the name, IP and port of the new contact. "
		+ "To add a public key to the contact for authenticated communication, please use the \"contacts update\" command."
		+ System.lineSeparator() + System.lineSeparator()
		+ "If it can not be added (e.g. because an entry of that name already exists) an error message is displayed. "
		+ "Syntax: \"contacts add <name> <ip> <port>\". "
		+ "Name and IP can be any String up to 255 characters in length. For a normal IPv4 Adress the regular format is used, e.g. \"127.0.0.1\". "
		+ "Port can be any Integer."),
	CONTACTS_UPDATE (" " // Formated to make the capturing groups of the regex clearer
			+ "(" // Because naming restrictions might change, the only name restriction enforced *here* is "no whitespaces" (for easy parsing)
			+  "(\\S{1,255} (?i)name(?-i) \\S{1,255})" // updating the name
			+ "|(\\S{1,255} (?i)ip(?-i) (\\d|\\.){1,255})"   // updating the ip
			+ "|(\\S{1,255} (?i)port(?-i) \\d+)"     // updating the port
			+ "|(\\S{1,255} (?i)pk(?-i) ((\".+\")|remove))" // updating the pk
			+ ")", 
		"Updates one entry in the communication list, given the name of the entry to update, the attribute to change and the new value."
		+ "If no update can be performed (e.g. no entry with the given name exists) an error message is displayed. "
		+ System.lineSeparator() + System.lineSeparator()
		+ "Syntax \"contacts update <name> <attr> <value>\". "
		+ "The argument <name> specifies the entry to change, "
		+ "<attr> may be either \"name\", \"ip\", \"port\" or \"pk\", specifying the attribute to change, "
		+ "<value> is the new value that the given attribute is to be set to. " + System.lineSeparator()
		+ "Example: \"contacts update Bob name Bobbie\"" 
		+ System.lineSeparator() + System.lineSeparator()
		+ "Adding a public key: If <attr> is \"pk\" then <value> will have to be the path to the public key file. "
		+ "The path will have to be given in quotation marks to avoid issues with white spaces, e.g. the command would be:" + System.lineSeparator()
		+ "contacts update Bob pk \"name_of_public_key_file\"" + System.lineSeparator()
		+ "Currently accepted file formats are: .pub  -  For now, please enter the file name without the extension" + System.lineSeparator()
		+ "The starting directory is " + System.getProperty("user.dir") + File.separator + "SignatureKeys" + File.separator
		+ System.lineSeparator() + System.lineSeparator()
		+ "If you wish to remove a public key of a contact, enter: \"contacts update <name> pk remove\""),
	CONTACTS_SHOWPK ("( \\S+)", "Shows the public key of specified user." + System.lineSeparator() + "Syntax: contacts showpk <user>"),
	
	// DEBUG COMMANDS, NOT INTENDED AS PART OF THE FINAL PRODUCT - DEVELOPER USE ONLY
	// FUNCTIONALITY NOT GUARANTEED
	DEBUG_GENSIGPAIR ("", "Generates a new public and private key pair for this machine."),
	DEBUG_SETPK ("( .+) (.+)", 
			"Manually sets the public key of a contact to whatever the user entered. "
			+ "This is done directly, no file is loaded." 
			+ System.lineSeparator() + " Syntax: debug setpk <user> <newpk>")
	
	;
	
	/** Unique name of the command, in lower case */
	private final String commandName;
	/** Regex pattern of the command, includes the command name and syntax of the arguments. 
	 * Command name is not case sensitive, arguments may or may not be depending on the exact command. */
	private final String commandPattern;
	/** The help text of a command describes its proper syntax in natural language, as well as its function. */
	private final String helpText;
	
	/**
	 * Constructor.
	 * @param argumentsSyntax
	 * 		Syntax of the arguments following the command name, given as a regular expression. <p>
	 * 		The arguments dictate the exact execution of the command. <p>
	 * 		(e.g. "help <i>contacts add</i>" returns additional information for the command "contacts add") <p>
	 * @param helpText
	 * 		The help text of a command describes its proper syntax in natural language, as well as its function.
	 */
	Command (String argumentsSyntax, String helpText) {
		this.commandName = this.name().toLowerCase().replace('_', ' ');
		// Command name is insensitive, rest of the command is case sensitive
		this.commandPattern = "(?i)" + commandName + "(?-i)" + argumentsSyntax;
		this.helpText = helpText;
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
	 * Intended to be used for debug / testing purposes, for pattern matching use {@link #match(String)} if possible.
	 * @return The regular expression describing this commands accepted syntax.
	 */
	public String getCommandPattern() {
		return commandPattern;
	}
	
	/**
	 * @return A text describing the function and syntax of this command.
	 */
	public String getHelp() {
		return this.helpText;
	}
	

}