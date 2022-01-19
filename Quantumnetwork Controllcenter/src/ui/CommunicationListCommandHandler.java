package ui;

import java.util.ArrayList;

import CommunicationList.Database;
import CommunicationList.DbObject;
import MessengerSystem.Authentication;

/**
 * The purpose of this class is to be used by {@link CommandHandler}, specifically for executing commands pertaining to the Communication List.
 * @author Sasha Petri
 */
class CommunicationListCommandHandler {
	
	private final static String SEE_CONSOLE = "Please see the system console for an error log.";
	/** What to enter in the CommunicationList as the public key of an entry which currently has no key associated with it (e.g. a new contact) */
	private final static String NO_KEY = "NO KEY SET";

	/**
	 * Handles the execution of the command {@link Command#CONTACTS_ADD}. 
	 * Adds the contact described in commandArgs to the {@link Database}
	 * @param commandArgs
	 * 		commandArgs[0] is the name of the contact to add <br>
	 * 		commandArgs[1] is the IP of the contact to add <br>
	 * 		commandArgs[2] is the port of the contact to add
	 * @return
	 * 		a String describing whether or not the contact was successfully added to the {@link Database}
	 */
	static String handleContactsAdd(String[] commandArgs) {
		String name = commandArgs[0], ip = commandArgs[1];
		int port = Integer.parseInt(commandArgs[2]);
		boolean success = Database.insert(name, ip, port, NO_KEY);
		if(success) {
			return "Successfully inserted new contact (" + name + ", " + ip + ", " + port + ") into the contact list."; 
		} else {
			return "ERROR - Could not add new contact to the contact list. " + SEE_CONSOLE;
		}
	}
	
	/**
	 * Handles the execution of the command {@link Command#CONTACTS_REMOVE}.
	 * Removes the contact with the name given in commandArgs from the {@link Database}.
	 * @param commandArgs
	 * 		commandArgs[0] is the name of the contact to remove
	 * @return
	 * 		a String describing whether or not the contact was successfully remove from the {@link Database}
	 */
	static String handleContactsRemove(String[] commandArgs) {
		String name = commandArgs[0];
		boolean success = Database.delete(name);
		if(success) {
			return "Successfully deleted the contact \"" + name + "\" from the contact list.";
		} else {
			return "ERROR - Could not remove the contact \"" + name +  "\" from the contact list. " + SEE_CONSOLE;
		}
	}
	
	/**
	 * Handles the execution of the command {@link Command#CONTACTS_SEARCH}.
	 * Searches for the contact with the name given in commandArgs in the {@link Database}.
	 * @param commandArgs
	 * 		commandArgs[0] is the name of the contact to search
	 * @return
	 * 		a String containing the contact's information if found <br>
	 * 		otherwise returns a String saying that the contact could not be found
	 */
	static String handleContactsSearch(String[] commandArgs) {
		String name = commandArgs[0];
		DbObject query = Database.query(name);
		if (query == null) {
			return "ERROR - Could not find a contact with the name \"" + name + "\" in the contact list. "+ SEE_CONSOLE;
		} else {
			return dbObjectToString(query);
		}
	}
	
	/**
	 * Handles the execution of the command {@link Command#CONTACTS_SHOW}.
	 * @return a String containing a list of all contacts in the {@link Database}
	 */
	static String handleContactsShow() {
		ArrayList<DbObject> entries = Database.queryAll();
		if (entries == null) {
			return "ERROR - There was a problem with querying the database. " + SEE_CONSOLE;
		} else {
			String out = "";
			for (DbObject entry : entries) {
				out += dbObjectToString(entry) + System.lineSeparator();
			}
			return out;
		}
	}
	
	/**
	 * Handles the execution of the command {@link Command#CONTACTS_UPDATE}.
	 * Updates an entry in the {@link Database}, as specified in commandArgs.
	 * @param commandArgs
	 * 		commandArgs[0] is the name of the contact to update <br>
	 * 		commandArgs[1] is one of the following Strings: "name","ip","port","pk" (not case sensitive)
	 * 		and decides which value is modified
	 * 		commandArgs[2] is the value that the specified attribute will be set to
	 * @return
	 * 		a String stating whether or not the update was successful
	 * @throws IllegalArgumentException
	 * 		if commandArgs is not of length 3
	 * @throws NumberFormatException
	 * 		if commandArgs[1] is "port" and commandArgs[2] can not be parsed to an integer
	 */
	static String handleContactsUpdate(String[] commandArgs) {
		// Check if entry actually exists, since we can't update a non-existant entry
		if(commandArgs.length != 3) 
			throw new IllegalArgumentException("The method handleContactUpdate(commandArgs) expects an array of size 3, but size was " + commandArgs.length + ".");
				
		String output;
		
		String oldName = commandArgs[0];
		DbObject entryToChange = Database.query(oldName);
		if (entryToChange == null) {
			output = "ERROR - Could not find a contact with the name \"" + commandArgs[0] + "\" in the contact list. " + SEE_CONSOLE;
		}
		
		String attributeToUpdate = commandArgs[1].toLowerCase();
		switch (attributeToUpdate) {
		case "name":
			String newName = commandArgs[2];
			boolean nameChangeSuccess = Database.updateName(oldName, newName);
			if (nameChangeSuccess) {
				output = "Successfully changed name of \"" + oldName + "\" to " + newName;
			} else {
				output = "ERROR - Could not change name of \"" + oldName + "\" to " + newName + ". " + SEE_CONSOLE;
			}
			break;
		case "ip":
			String newIP = commandArgs[2];
			boolean ipChangeSuccess = Database.updateIP(oldName, newIP);
			if (ipChangeSuccess) {
				output = "Successfully changed IP of \"" + oldName + "\" to " + newIP;
			} else {
				output = "ERROR - Could not change IP of \"" + oldName + "\" to " + newIP + ". " + SEE_CONSOLE;
			}
			break;
		case "port":
			int newPort = Integer.parseInt(commandArgs[2]);
			boolean portChangeSuccess = Database.updatePort(oldName, newPort);
			if (portChangeSuccess) {
				output = "Successfully changed Port of \"" + oldName + "\" to " + newPort;
			} else {
				output = "ERROR - Could not change Port of \"" + oldName + "\" to " + newPort + ". " + SEE_CONSOLE;
			}
			break;
		case "pk": // Updating the pk associated with a contact
			// comandArgs[2] will either be >>remove<< or of the form >>".+"<<   ((>> << used as quotation marks here))
			// Behaviour is dependent on which of these two it is
			
			if(commandArgs[2].equals("remove")) { // User wishes to remove the pk associated with this contact
				boolean pkDeleteSuccess = Database.updateSignatureKey(oldName, NO_KEY);
				if (pkDeleteSuccess) {
					output = "Successfully updated the communication list. Contact \"" + oldName + "\" no longer has a public key associated with them.";
				} else {
					output = "ERROR - Could not update the contact \"" + oldName + "\" to no longer have a public key assosicated with them. " + SEE_CONSOLE;
				}
			} else { // User wishes to update the pk associated with this contact / add a pk if not present
				String pkLocation = commandArgs[2].substring(1, commandArgs[2].length() - 1); // get the location without the "" around it
				String pkString = Authentication.readPublicKeyStringFromFile(pkLocation); // load the pk itself
				if(pkString == null) { // If there was an error loading the pk from the file
					output = "ERROR - Could not load the public key at location: \"" + pkLocation + "\". " + SEE_CONSOLE;
				} else { // If the pk could be loaded, try to insert it into the data base
					boolean pkChangeSuccess = Database.updateSignatureKey(oldName, pkString);
					if(pkChangeSuccess) {
						output = "Successfully changed public key associated with \"" + oldName + "\".";
					} else {
						output =  "Could not change the public key associated with \"" + oldName + "\" " + SEE_CONSOLE;
					}
				}	
			}
			break;
		default:
			output = "ERROR - Can not update the attribute \"" + attributeToUpdate + "\". This functionality has either not been implemented yet, or something went wrong.";
		}
		
		return output;
	}
	
	private static String dbObjectToString(DbObject dbo) {
		return "[Name: " + dbo.getName() + " IP: " + dbo.getIpAddress() + " Port: " + dbo.getPort() + "]";
	}
}
