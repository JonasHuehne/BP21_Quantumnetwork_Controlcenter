package ui;

import java.util.ArrayList;

import CommunicationList.Database;
import CommunicationList.DbObject;

class CommunicationListCommandHandler {
	
	private final static String SEE_CONSOLE = "Please see the system console for an error log.";

	static String handleContactsAdd(String[] commandArgs) {
		String name = commandArgs[0], ip = commandArgs[1];
		int port = Integer.parseInt(commandArgs[2]);
		boolean success = Database.insert(name, ip, port);
		if(success) {
			return "Successfully inserted new contact (" + name + ", " + ip + ", " + port + ") into the contact list."; 
		} else {
			return "Could not add new contact to the contact list. " + SEE_CONSOLE;
		}
	}
	
	static String handleContactsRemove(String[] commandArgs) {
		String name = commandArgs[0];
		boolean success = Database.delete(name);
		if(success) {
			return "Successfully deleted the contact \"" + name + "\" from the contact list.";
		} else {
			return "Could not remove the contact \"" + name +  "\" from the contact list. " + SEE_CONSOLE;
		}
	}
	
	static String handleContactsSearch(String[] commandArgs) {
		String name = commandArgs[0];
		DbObject query = Database.query(name);
		if (query == null) {
			return "Could not find a contact with the name \"" + name + "\" in the contact list. "+ SEE_CONSOLE;
		} else {
			return "Found the contact: " + dbObjectToString(query);
		}
	}
	
	static String handleContactsShow() {
		ArrayList<DbObject> entries = Database.queryAll();
		if (entries == null) {
			return "There was a problem with querying the database. " + SEE_CONSOLE;
		} else {
			String out = "";
			for (DbObject entry : entries) {
				out += dbObjectToString(entry) + System.lineSeparator();
			}
			return out;
		}
	}
	
	static String handleContactsUpdate(String[] commandArgs) {
		// Check if entry actually exists, since we can't update a non-existant entry
		String oldName = commandArgs[0];
		DbObject entryToChange = Database.query(oldName);
		if (entryToChange == null) {
			return "Could not find a contact with the name \"" + commandArgs[0] + "\" in the contact list. " + SEE_CONSOLE;
		}
		
		String attributeToUpdate = commandArgs[1];
		switch (attributeToUpdate) {
		case "name":
			String newName = commandArgs[2];
			boolean nameChangeSuccess = Database.updateName(oldName, newName);
			if (nameChangeSuccess) {
				return "Successfully changed name of \"" + oldName + "\" to " + newName;
			} else {
				return "Could not change name of \"" + oldName + "\" to " + newName + ". " + SEE_CONSOLE;
			}
		case "ip":
			String newIP = commandArgs[2];
			boolean ipChangeSuccess = Database.updateIP(oldName, newIP);
			if (ipChangeSuccess) {
				return "Successfully changed IP of \"" + oldName + "\" to " + newIP;
			} else {
				return "Could not change IP of \"" + oldName + "\" to " + newIP + ". " + SEE_CONSOLE;
			}
		case "port":
			int newPort = Integer.parseInt(commandArgs[2]);
			boolean portChangeSuccess = Database.updatePort(oldName, newPort);
			if (portChangeSuccess) {
				return "Successfully changed Port of \"" + oldName + "\" to " + newPort;
			} else {
				return "Could not change Port of \"" + oldName + "\" to " + newPort + ". " + SEE_CONSOLE;
			}
		default:
			return "ERROR - Can not update the attribute \"" + attributeToUpdate + "\". This functionality has either not been implemented yet, or something went wrong.";
		}
	}
	
	private static String dbObjectToString(DbObject dbo) {
		return "[Name: " + dbo.getName() + " IP: " + dbo.getIpAddr() + " Port: " + dbo.getPort() + "]";
	}
}
