package ui;

import java.util.ArrayList;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.QuantumnetworkControllcenter;

/**
 * Handles the execution of Debug Commands.
 * Not guaranteed to work, these Commands are intended to be used by developers only.
 * @author Sasha Petri
 * 
 * @deprecated Due to time concerns, the focus of developement has shifted to the GUI. 
 * Support for the Console UI may be picked up again later, but at the moment there is no guarantee for it to be up to date or functional.
 */
@Deprecated
public class DebugCommandHandler {

	/**
	 * Handles {@link Command#DEBUG_GENSIGPAIR}.
	 * @return
	 * 		a message whether or not the execution was successful
	 */
	static String handleGenSigPair() {
		boolean b = QuantumnetworkControllcenter.authentication.generateSignatureKeyPair();
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
		
		// Clears connection manager to avoid issues if there are already connections bob and alice
		QuantumnetworkControllcenter.conMan.destroyAllConnectionEndpoints();
		
		String[] commandsToRun 
			= new String[] {
					
					// Simulate "Bob" adding "Alice" as a contact - Bob knows Alice wants to receive messages on 17141
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
		
		out.append("EXPLANATION: " + System.lineSeparator()
				+ "Interpreting this local, cyclical connection might initially be a bit confusing. "
				+ "To explain: This simulates Alice and Bob establishing a connection. "
				+ "Each has one connection endpoint, named after the other person. " + System.lineSeparator()
				+ "So, the connection Endpoint \"Alice\" is the connection Endpoint that Bob has, which is connected to Alice. "
				+ "Bob will receive messages from Alice on it. " + System.lineSeparator()
				+ "Vice versa, Alice will receive her messages from Bob on connection endpoint \"Bob\". " + System.lineSeparator()
				+ "This means if you were to enter \"" + Command.HELLO_WORLD.getCommandName() + " Bob\" (which sends a hello world to the contact named Bob)"
				+ ", it would cause a message to appear on the CE named \"Alice\" because in this simulation, that is where Bob receives messages (from Alice)." + System.lineSeparator()
				+ "In this setting, this might be a bit of a mind-bend, but if we are actually establishing connections "
				+ "to other users of the network, the connection being named after the user / contact will be intuitive." + System.lineSeparator());
		
		return out.toString();
	}

	/**
	 * Handles execution of {@link Command#DEBUG_CLEAR_COMMLIST}
	 * @return
	 * 		the new communication list
	 */
	public static String handleClearCommList() {
		ArrayList<Contact> contacts = QuantumnetworkControllcenter.communicationList.queryAll();
		for (Contact c : contacts) QuantumnetworkControllcenter.communicationList.delete(c.getName());
		String out = "Communication List now looks like this (should be empty): " + System.lineSeparator() + CommandHandler.processCommand(Command.CONTACTS_SHOW.getCommandName());
		return out;
	}
	
	/**
	 * Handles execution of the {@link Command#SPAM}
	 * @return
	 * 		a spammy long string
	 */
	public static String handleSpam() {
		char[] abc = "abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVXYZ".toCharArray();
		char[] nums = "123456789".toCharArray();
		StringBuilder out = new StringBuilder();
		for (char c : abc) {
			out.append(c + System.lineSeparator());
			out.append(new String(nums) + System.lineSeparator());
		}
		return out.toString();
	}
}
