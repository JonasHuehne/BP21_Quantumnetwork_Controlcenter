package ui;

import frame.QuantumnetworkControllcenter;
import messengerSystem.SHA256withRSAAuthentication;

public class DebugCommandHandler {

	static String handleGenSigPair() {
		boolean b = SHA256withRSAAuthentication.generateSignatureKeyPair();
		String out;
		if (b) {
			out = "Successfully generated a key pair for Authentication.";
		} else {
			out = "Could not generate key pair, see console for an error log.";
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
		
		boolean success = QuantumnetworkControllcenter.communicationList.updateSignatureKey(commandArgs[0], commandArgs[1]);
		
		if (success) {
			out = "Successfully changed pk of contact \"" + commandArgs[0] + "\" to: " + System.lineSeparator() + commandArgs[1];
		} else {
			out = "ERROR - Could not change pk of contact \"" + commandArgs[0] + "\".";
		}
		
		return out;
	}
}
