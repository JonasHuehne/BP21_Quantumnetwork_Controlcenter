package ui;

import java.io.File;

final class CommandConstants {

	enum Category {
		GENERAL ("General"),
		COMMUNICATION_LIST ("Communication List Management"),
		CONNECTIONS ("Connection Management"),
		MESSAGING ("Message and Data Transfer"),
		DEBUG ("Debug Commands - No Guarantees");
		
		private final String name;
		
		Category(String name) {
			this.name = name;
		}
		
		public String getCategoryName() {
			return name;
		}
	}
	
	/*
	 * REGEX for each command.
	 * Used by the CommandParser when checking whether a command is valid.
	 */
	
	public static final String PATTERN_HELP
		= "( .+)?";
	public static final String PATTERN_CONTACTS_SHOW
		= "";
	public static final String PATTERN_CONTACTS_SEARCH
		= " \\S{1,255}";
	public static final String PATTERN_CONTACTS_REMOVE
		= " \\S{1,255}";
	public static final String PATTERN_CONTACTS_ADD
		= " \\S{1,255} (\\d|\\.){1,255} \\d+";
	public static final String PATTERN_CONTACTS_UPDATE
		= " " // Formated to make the capturing groups of the regex clearer
		+ "(" // Because naming restrictions might change, the only name restriction enforced *here* is "no whitespaces" (for easy parsing)
		+ "(\\S{1,255} (?i)name(?-i) \\S{1,255})" 			// updating the name
		+ "|(\\S{1,255} (?i)ip(?-i) (\\d|\\.){1,255})"   	// updating the ip
		+ "|(\\S{1,255} (?i)port(?-i) \\d+)"     			// updating the port
		+ "|(\\S{1,255} (?i)pk(?-i) ((\".+\")|remove))" 	// updating the pk
		+ ")";
	public static final String PATTERN_CONTACTS_SHOWPK
		= "( \\S+)";
	public static final String PATTERN_CONNECTIONS_ADD
		= "( \\S+)( \\d{1,5})?";
	public static final String PATTERN_CONNECTIONS_REMOVE
		= "( \\S+)";
	public static final String PATTERN_CONNECTIONS_SHOW
		= "";
	public static final String PATTERN_CONNECTIONS_CLOSE
		= "( \\S+)";
	public static final String PATTERN_CONNECTIONS_CONNECT_TO
		= "( \\S+)";
	public static final String PATTERN_CONNECTIONS_WAIT_FOR
		= "( \\S+)";
	public static final String PATTERN_CONNECTIONS_HELLO_WORLD
		= "( \\S+)";
	public static final String PATTERN_DEBUG_GENSIGPAIR
		= "";
	public static final String PATTERN_DEBUG_SETPK
		= "( .+) (.+)";
	public static final String PATTERN_DEBUG_SETUP_LOCAL_CONS
		= "";
	public static final String PATTERN_DEBUG_CLEAR_COMMLIST
		= "";
	public static final String PATTERN_DEBUG_SPAM
		= "";
	
	/*
	 * Human-readable argument syntax for each command. 
	 * Displayed when "help <commandName>" is entered to give the user a brief overview of the command's structure.
	 */
	public static final String HR_ARGS_HELP
		= " [command]";
	public static final String HR_ARGS_CONTACTS_SHOW
		= "";
	public static final String HR_ARGS_CONTACTS_SEARCH
		= " <contact name>";
	public static final String HR_ARGS_CONTACTS_REMOVE
		= " <contact name>";
	public static final String HR_ARGS_CONTACTS_ADD
		= " <name> <ip> <port>";
	public static final String HR_ARGS_CONTACTS_UPDATE
		= " <name> <attr> <value> ";
	public static final String HR_ARGS_CONTACTS_SHOWPK
		= " <name>";
	public static final String HR_ARGS_CONNECTIONS_ADD
		= " <name>";
	public static final String HR_ARGS_CONNECTIONS_REMOVE
		= " <connection ID>";
	public static final String HR_ARGS_CONNECTIONS_SHOW
		= "";
	public static final String HR_ARGS_CONNECTIONS_CLOSE
		= " <connection ID>";
	public static final String HR_ARGS_CONNECTIONS_CONNECT_TO
		= " <connection ID>";
	public static final String HR_ARGS_CONNECTIONS_WAIT_FOR
		= " <connection ID>";
	public static final String HR_ARGS_CONNECTIONS_HELLO_WORLD
		= " <connection ID>";
	public static final String HR_ARGS_DEBUG_GENSIGPAIR
		= "";
	public static final String HR_ARGS_DEBUG_SETPK
		= " <name> <pk>";
	public static final String HR_ARGS_DEBUG_SETUP_LOCAL_CONS
		= "";
	public static final String HR_ARGS_DEBUG_CLEAR_COMMLIST
		= "";
	public static final String HR_ARGS_DEBUG_SPAM
		= "";
	
	/*
	 * Mini help for each command.
	 * Displayed when "help" is entered, next to the name of each command.
	 */
	public static final String HELP_SHORT_HELP
		= "Displays all Commands, or info on individual commands.";
	public static final String HELP_SHORT_CONTACTS_SHOW
		= "Shows all contacts in the communication list.";
	public static final String HELP_SHORT_CONTACTS_SEARCH
		= "Searches for a specific contact in the communication list.";
	public static final String HELP_SHORT_CONTACTS_REMOVE
		= "Removes a specific contact from the communication list.";
	public static final String HELP_SHORT_CONTACTS_ADD
		= "Adds a specific contact to the communication list.";
	public static final String HELP_SHORT_CONTACTS_UPDATE
		= "Updates a specific contact in the communication list.";
	public static final String HELP_SHORT_CONTACTS_SHOWPK
		= "Shows the full pk of a specific contact from the communication list.";
	public static final String HELP_SHORT_CONNECTIONS_ADD
		= "Adds a network connection to the specified contact.";
	public static final String HELP_SHORT_CONNECTIONS_REMOVE
		= "Removes a connection from the list of connections.";
	public static final String HELP_SHORT_CONNECTIONS_SHOW
		= "Shows the list of connections.";
	public static final String HELP_SHORT_CONNECTIONS_CLOSE
		= "Closes a specified connection.";
	public static final String HELP_SHORT_CONNECTIONS_CONNECT_TO
		= "Sends a connection request to a specified connection.";
	public static final String HELP_SHORT_CONNECTIONS_WAIT_FOR
		= "Causes a specified connection to wait for incoming connection requests.";
	public static final String HELP_SHORT_CONNECTIONS_HELLO_WORLD
		= "Sends a simple hello world message to a specified connection.";
	public static final String HELP_SHORT_DEBUG_GENSIGPAIR
		= "Generates a (pk, sk) pair for debugging purposes.";
	public static final String HELP_SHORT_DEBUG_SETPK
		= "Sets the pk of a specified contact to a given value.";
	public static final String HELP_SHORT_DEBUG_SETUP_LOCAL_CONS
		= "Sets up a cyclical local connection for testing purposes.";
	public static final String HELP_SHORT_DEBUG_CLEAR_COMMLIST
		= "Completely clears the communication list of all entries.";
	public static final String HELP_SHORT_DEBUG_SPAM
		= "Does nothing.";
	
	/*
	 * Full help text for each command.
	 * Explains the functionality of the command in detail.
	 */
	
	public static final String HELP_LONG_HELP
		= "Displays a list of all commands, if just \"help\" is entered. "
		+ "If help is entered followed by the name of a command, it displays additional info on that command. "
		+ "When describing the syntax of a command <argument> describes a mandatory argument, while [argument] describes an optional argument.";
	public static final String HELP_LONG_CONTACTS_SHOW
		= "Shows all contacts in the communication list.";
	public static final String HELP_LONG_CONTACTS_SEARCH
		= "Searches for one entry in the communication list by name, displaying it if found. "
		+ "If no entry of that name is found, an error message is displayed. ";
	public static final String HELP_LONG_CONTACTS_REMOVE
		= "Removes one entry in the communication list, given the name of the contact. "
		+ "If no entry of that name is found, an error message is displayed. ";
	public static final String HELP_LONG_CONTACTS_ADD
		= "Adds one entry to the communication list, given the name, IP and port of the new contact. "
		+ "If the contact can not be added, an error message is displayed. "
		+ "To add a public key to the contact for authenticated communication, please use the \"contacts update\" command."
		+ "Regarding the syntax: " + System.lineSeparator()
		+ "Names can be alphanumerical, currently lengths of up to 255 are supported, but < 20 is encouraged. "
		+ "IPs are currently permitted to be any IPv4 address. Port may be any TCP port.";
	public static final String HELP_LONG_CONTACTS_UPDATE
		= "Updates one entry in the communication list, given the name of the entry to update, the attribute to change and the new value."
		+ "If no update can be performed (e.g. no entry with the given name exists) an error message is displayed. "
		+ "Regarding the syntax: " + System.lineSeparator()
		+ "The argument <name> specifies the entry to change, "
		+ "<attr> may be either \"name\", \"ip\", \"port\" or \"pk\", specifying the attribute to change, "
		+ "<value> is the new value that the given attribute is to be set to. " + System.lineSeparator()
		+ "Example: \"contacts update Bob name Bobbie\"" 
		+ System.lineSeparator() + System.lineSeparator()
		+ "Adding a public key: If <attr> is \"pk\" then <value> will have to be the path to the public key file. "
		+ "The path will have to be given in quotation marks to avoid issues with white spaces, e.g. the command would be:" + System.lineSeparator()
		+ "contacts update Bob pk \"name_of_public_key_file\"" + System.lineSeparator()
		+ "Currently accepted file formats are: .pub, .pem, .key, .der, .txt or no extension." + System.lineSeparator()
		+ "The starting directory is " + System.getProperty("user.dir") + File.separator + "SignatureKeys" + File.separator
		+ System.lineSeparator() + System.lineSeparator()
		+ "If you wish to remove a public key of a contact, enter: \"contacts update <name> pk remove\"";
	public static final String HELP_LONG_CONTACTS_SHOWPK
		= "Shows the full pk of a specific contact from the communication list.";
	public static final String HELP_LONG_CONNECTIONS_ADD
		= "Creates a new connection endpoint, which through the use of the "
		+ "\"connect to\" and \"wait for\" commands can be connected to another (remote) connection endpoint. "
		+ "This then establishes a connection that can be used to transfer messages and generate keys together with the other party. "
		+ "Regarding the syntax: " + System.lineSeparator()
		+ "Name is the name of any contact in the communication list, and will then be the name of the connection endpoint (the connection id). "
		+ "Port may be any TCP port, or be left empty to use the default port " + 0 + ". "
		// TODO Adjust this after the merge with the console control of connections branch
		+ "The specified port is the port that this connection endpoint will be listening for messages on, once active. "
		+ "Outgoing messages will be sent to whichever IP and port is associated with the given contact.";
	public static final String HELP_LONG_CONNECTIONS_REMOVE
		= "Removes a connection endpoint. If it is part of a currently active connection, that connection is closed. ";
	public static final String HELP_LONG_CONNECTIONS_SHOW
		= "Shows a list of all connections, as well as their current status.";
	public static final String HELP_LONG_CONNECTIONS_CLOSE
		= "Closes the specified connection.";
	public static final String HELP_LONG_CONNECTIONS_CONNECT_TO
		= "Attempts to open the specified connection for the exchange of messages. "
		+ "Specifically, this works by sending a request to this connection's specified IP and Port. "
		+ "If the machine with that IP currently has a local connection endpoint waiting for a request on that port, "
		+ "then the connection will be established. Otherwise, this command will fail to establish a connection."
		+ System.lineSeparator()
		+ "Example: If you want to connect to IP 125.55.55.235 on port 2222, "
		+ "the contact with that IP will need a connection endpoint with the local port 2222.";
	public static final String HELP_LONG_CONNECTIONS_WAIT_FOR
		= "Makes the specified connection endpoint wait for an incoming connection request. "
		+ "Requires the connection to be closed first.";
	public static final String HELP_LONG_CONNECTIONS_HELLO_WORLD
		= "Sends a simple hello world message along the specified connection. Used for debugging & sanity testing. ";
	public static final String HELP_LONG_DEBUG_GENSIGPAIR
		= "Generates a (pk, sk) pair for debugging purposes.";
	public static final String HELP_LONG_DEBUG_SETPK
		= "Sets the pk of a specified contact to a given value.";
	public static final String HELP_LONG_DEBUG_SETUP_LOCAL_CONS
		= "Sets up a cyclical local connection for testing purposes by executing other connection commands. Displays their output.";
	public static final String HELP_LONG_DEBUG_CLEAR_COMMLIST
		= "Completely clears the communication list of all entries.";
	public static final String HELP_LONG_DEBUG_SPAM
		= "Does nothing. Used solely for testing how the console reacts to very long outputs."
		+ System.lineSeparator() + "The alphabet goes:" 
		+ System.lineSeparator() + "A" + System.lineSeparator() + "B"
		+ System.lineSeparator() + "C" + System.lineSeparator() + "D" + System.lineSeparator() + "E"
		+ System.lineSeparator() + "F" + System.lineSeparator() + "G" + System.lineSeparator() + "H"
		+ System.lineSeparator() + "I" + System.lineSeparator() + "J" + System.lineSeparator() + "K"
		+ System.lineSeparator() + "L" + System.lineSeparator() + "M" + System.lineSeparator() + "N"
		+ System.lineSeparator() + "O" + System.lineSeparator() + "P" + System.lineSeparator() + "Q"
		+ System.lineSeparator() + "R" + System.lineSeparator() + "S" + System.lineSeparator() + "T"
		+ System.lineSeparator() + "U" + System.lineSeparator() + "V" + System.lineSeparator() + "W"
		+ System.lineSeparator() + "X" + System.lineSeparator() + "Y" + System.lineSeparator() + "Z"
		+ System.lineSeparator() + "I would also like to introduce you to the numbers 1 through 20, 20 times" + System.lineSeparator()
		+ ("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20" + System.lineSeparator()).repeat(20);
}

