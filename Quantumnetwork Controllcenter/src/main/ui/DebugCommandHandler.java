package ui;

import java.util.ArrayList;

import communicationList.CommunicationList;
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

	/**
	 * Handles {@link Command#DEBUG_SETPK}
	 * @return
	 * 	the output of the individually run commands
	 */
	public static String handleSetUpLocalConnections() {
		
		// Clear Communication List before running this command.
		handleClearCommList();
		
		String[] commandsToRun 
			= new String[] {
					// Simulate "Bob" adding "Bob" as a contact - Bob knows Alice wants to receive messages on 17141
					"contacts add Alice 127.0.0.1 17141",	
					
					// Simulate "Bob" creating a connection endpoint which he wishes to use for communication with Alice
					// Bob wishes to receive messages on Port 17142
					// Outgoing messages will be sent to Alice IP and to Port 17141 (port of contact Alice)
					"connections add Alice 17142", 
					
					// Simulate "Alice" adding "Bob" as a contact - Alice knows Bob wants to receive messages on port 17142
					"contacts add Bob 127.0.0.1 17142",
					
					// Simulate "Alice" adding creating a connection endpoint which she wishes to use for communication with Bob
					// Alice wants to receive messages on Port 17141
					"connections add Bob 17141",
					
					// Now, one of the two parties has to wait for a connection - we will say it is Alice, waiting for Bob
					Command.WAIT_FOR.getCommandName() + " Bob",
					
					/*
					 * Now, if Bob attempts to start a connection to Alice, the connection request should be accepted,
					 * because Alice is waiting for a connection on that port.
					 * TODO: Check with Jonas (or do manual testing) that if Alice only has CE "Bob" waiting, only 
					 * 		 connection requests from that IP will be accepted. Otherwise there might be confusing behaviour.
					 */
					Command.CONNECT_TO.getCommandName() + " Alice",
					
					/*
					 * Now the two parties should be able to send each other messages
					 */
					
					// Finally display all connections and their state
					Command.CONNECTIONS_SHOW.getCommandName()
			};
		StringBuilder out = new StringBuilder();
		for (String s : commandsToRun) {
			out.append("--- ENTER COMMAND: " + s + " ---" 	+ System.lineSeparator());
			out.append(CommandHandler.processCommand(s) 	+ System.lineSeparator());
			out.append("--- END OF COMMAND: " + s + " ---" 	+ System.lineSeparator());
			out.append(System.lineSeparator());
		}
		
		return out.toString();
	}

	public static String handleClearCommList() {
		
		ArrayList<String> names = new ArrayList<String>();
		
		CommunicationList cl = QuantumnetworkControllcenter.communicationList;
		
		cl.queryAll().forEach((entry) -> {names.add(entry.getName());});;
		
		names.forEach((s) -> cl.delete(s));
		
		String out = "Communication List now looks like this: " + System.lineSeparator() + CommandHandler.processCommand(Command.CONTACTS_SHOW.getCommandName());
		
		return out;
	}
}
