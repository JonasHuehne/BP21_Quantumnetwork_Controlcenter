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
}
