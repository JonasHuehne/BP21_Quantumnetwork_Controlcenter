package ui;

import CommunicationList.Database;
import CommunicationList.DbObject;
import MessengerSystem.Authentication;

public class DebugCommandHandler {

	static String handleGenSigPair() {
		boolean b = Authentication.generateSignatureKeyPair();
		String out;
		if (b) {
			out = "Successfully generated a key pair for Authentication.";
		} else {
			out = "Could not generate key pair, see console for an error log.";
		}
		return out;
	}

	public static String handleShowPk(String[] commandArgs) {
		if(commandArgs.length != 1) {
			throw new IllegalArgumentException("showPk must be called with exactly one argument, but was instead called with " + commandArgs.length);
		}
		
		String out;
		
		DbObject user  = Database.query(commandArgs[0]);
		
		
		if(user == null) {
			out =  "No such contact : " + commandArgs[0];
		} else {
			out = "Public Key associated with contact "  + commandArgs[0] + " is: " + System.lineSeparator() + user.getSignatureKey();
		}
		
		return out;
	}

	/**
	 * Handles {@link Command#DEBUG_SETPK}
	 * @param commandArgs
	 * 		expected size: 2 <br>
	 * 		commandArgs[0] name of the contact
	 * 		commandsArgs[1] new pk of the contact, given directly as a string
	 * @return
	 * 		a message whether or not the change was successful
	 */
	public static String handleSetPk(String[] commandArgs) {
		String out;
		
		if(commandArgs.length != 2) throw new IllegalArgumentException("Illegal commandArgs array size, expected 2, got: " + commandArgs.length);
		
		boolean success = Database.updateSignatureKey(commandArgs[0], commandArgs[1]);
		
		if (success) {
			out = "Successfully changed pk of contact \"" + commandArgs[0] + "\" to: " + System.lineSeparator() + commandArgs[1];
		} else {
			out = "ERROR - Could not change pk of contact \"" + commandArgs[0] + "\".";
		}
		
		return out;
	}
}
