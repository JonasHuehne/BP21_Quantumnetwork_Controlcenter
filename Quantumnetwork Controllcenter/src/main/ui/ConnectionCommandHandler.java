package ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;import java.util.Comparator;

import java.util.Map;
import java.util.Map.Entry;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;
import networkConnection.TransmissionTypeEnum;

public class ConnectionCommandHandler {
	
	static CommunicationList communicationList = QuantumnetworkControllcenter.communicationList;
	
	/** When establishing a new connection, this is the default port */
	public static final int DEFAULT_PORT = 17144;
	
	/*
	 * Regarding the name of ConnectionEndpoints:
	 * For this implementation I have chosen to name them after the contact they are connecting to.
	 */
	
	/**
	 * Handles execution of {@link Command#CONNECTIONS_ADD}. <br>
	 * Adds a new connection / connection endpoint to the list, as determined by the arguments given in commandArgs. <br>
	 * New connection endpoint will be inactive after creation.
	 * @param commandArgs
	 * 		may be of length 1 or 2 <br>
	 * 		
	 * 		commandArgs[0] must be the name of a contact in the {@link CommunicationList}.
	 * 		The connection ID of the new connection will be the equal to the contact name, 
	 * 		the remote IP and remote port will be determined by the contact's IP and port. 
	 * 		No two connections to one contact will be created. <br>
	 * 
	 * 		commandArgs[1] is optional. If it exists, it decides which port this 
	 * 		new connection will be listening for messages on. Must be a valid TCP port.
	 * @return
	 * 		a message describing whether or not execution of the command was successful
	 */
	protected static String handleConnectionsAdd(String[] commandArgs) {
		
		String contactName;
		int localPort;
		
		// Command Arguments must either be just one argument (contact name) or two (contact name, local port)
		if (commandArgs.length == 1) {
			contactName = commandArgs[0];
			localPort = DEFAULT_PORT;
		} else if (commandArgs.length == 2) {
			contactName = commandArgs[0];
			localPort = Integer.parseInt(commandArgs[1]); // NumberFormatException if not int-string
		} else {
			throw new IllegalArgumentException("Expected commandArgs to be of length 1 or 2, but it was of length " + commandArgs.length);
		}
		
		// Don't accept illegal ports
		if (localPort < 0 || localPort > 65535) {
			return "ERROR - Could not create a connection where the local Endpoint is listening on port " + localPort + ". "
					+ "TCP Ports must be between 0 and 65535.";
			// Not throwing an illegal argument exception here because this is more likely caused by bad input than programmatic mistakes
		} else if (QuantumnetworkControllcenter.conMan.isPortInUse(localPort)) {
			return "ERROR - Can not create a connection listening on port " + localPort + " only one connection may listen on a port at a time.";
		}
		
		// Make sure given contact exists
		Contact contact = QuantumnetworkControllcenter.communicationList.query(contactName);
		if (contact == null) {
			return "ERROR - Can not create a connection to contact \"" + contactName + "\" - no such contact could be found in the communication list.";
		} 
		
		// Don't connect to the same contact twice
		/*
		 *  TODO: Theoretically this is already checked in conMan.createNewConnectionEndpoint(...)
		 *  however, currently (27.01.2022) the method outputs null either if a CE of that name already exists
		 *  or if there is already a CE in the CM listening on the given local port. Just by receiving null
		 *  the caller can't tell which case it is though, so we check separately here to give clear error messages.
		 */
		
		if (QuantumnetworkControllcenter.conMan.hasConnectionEndpoint(contactName)) {
			return "ERROR - Can not create a connection to contact \"" + contactName + "\" - such a connection already exists.";
		} 
		
		// If contact exists, and there is no connection, add the new connection	
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(contactName, localPort);	
		
		if (localPoint == null) {
			return "ERROR - Could not create the specified connection. Something went wrong, please see the system console in case there is an error log. ";
		} else {
			return "Successfully created connection to contact \"" + contactName + "\", listening on port " + localPort + ". "
					+ "The connection is still inactive. Use the command \"" + Command.CONNECT_TO + "\" to open the connection."; 
		}
		

	}
	
	/**
	 * Handles execution of {@link Command#CONNECTIONS_SHOW}.
	 * @return
	 * 		a string formatted to be a table of all connections <br>
	 * 		if no connections exists, a string stating there are no connections
	 */
	protected static String handleConnectionShow() {
		
		Map<String, ConnectionEndpoint> endpoints = QuantumnetworkControllcenter.conMan.returnAllConnections();
		
		if (endpoints.isEmpty()) {
			return "This machine currently has no connections.";
		}
		
		StringBuilder output = new StringBuilder();
		
		// Used to build a table
		String format = 
				"| %-24s | %-20s | %-10s | %-20s | %-22s | %-20s | %-22s |";	
		String header = String.format(format, 
				"ID", "STATUS", "Local Port", "Remote IP (Contact)", "Remote Port (Contact)", "Remote IP (Latest)", "Remote Port (Latest)");
		
		output.append(header);
		output.append(System.lineSeparator());
		
		ArrayList<Map.Entry<String, ConnectionEndpoint>> endpointsList = new ArrayList<>();
		
		// Sort the list alphabetically for prettier presentation 
		Collections.sort(endpointsList, 
				new Comparator<Map.Entry<String, ConnectionEndpoint>>() {
					@Override
					public int compare(Entry<String, ConnectionEndpoint> entryA, Entry<String, ConnectionEndpoint> entryB) {
						return entryA.getKey().compareTo(entryB.getKey());
				}
		});
		
		// For each Endpoint add an entry to the table
		for (Map.Entry<String, ConnectionEndpoint> endpoint : endpoints.entrySet()) {
			String connectionID = endpoint.getKey();
			String connectionState = endpoint.getValue().reportState().toString();
			int localPort = endpoint.getValue().getServerPort();
			
			
			Contact contact = communicationList.query(connectionID);
			
			if (contact == null) throw new IllegalArgumentException(
					"Attempted to access contact named " + connectionID + " however, no such contact was found. "
					+ "Each connection ID must correspond to the name of a contact in the communication list, because that contacts IP and Port will be used for the connection.");
			
			String contactRemoteIP = contact.getIpAddress(); // ip of the contact associated with this connection ID
			int contactRemotePort = contact.getPort();	// port the contact would like to receive messages on
			String latestRemoteIP = endpoint.getValue().getRemoteAddress(); // latest IP this connection has connected to
			int latestRemotePort = endpoint.getValue().getRemotePort(); // remote port (port messages are sent to) of the latest connection made
			
			// the two "latest" values may be "" and 0 if no connection has been established yet
			
			output.append(
					String.format(format, 
					connectionID, connectionState, localPort, contactRemoteIP, contactRemotePort, latestRemoteIP, latestRemotePort)
					);
			// TODO: TEMPORARY - FOR DEBUGGING ONLY:
			output.append(" Message Stack Size: " + endpoint.getValue().sizeOfMessageStack());
			output.append(System.lineSeparator());
		}
		
		// Give the user some info about what the columns mean
		String info;		
		info = 		System.lineSeparator()
					+	"ID: Identifies this connection / connection endpoint. This should be the same as the name of the contact to which this is a connection." 
					+ 	System.lineSeparator()
					+	"STATUS: Status of this connection (e.g. CONNECTED, CLOSED, ...)." 
					+ 	System.lineSeparator()
					+	"Local Port: Port that this endpoint expects incoming messages on. " 
					+ 	System.lineSeparator()
					+	"Remote IP (Contact): IP of the contact associated with this connection. "
					+ 	"Messages, including connection requests for this connection, will be sent to this IP." 
					+ 	System.lineSeparator()
					+	"Remote Port (Contact): Port entry of the contact associated with this connection. "
					+ 	"Messages, including connection requests for this connection, will be sent to this port." 
					+ 	System.lineSeparator()
					+	"Remote IP (Latest): Latest IP that this connection endpoint has connected to. "
					+ 	"May be unset if this connection was never active."
					+ 	System.lineSeparator()
					+	"Remote Port (Latest): Remote port associated with the last connection made on this connection endpoint. "
					+ 	"If there has been no connection, it will be 0.";
		
		output.append(info);
		
		return output.toString();
	}
	
	/**
	 * Handles execution of {@link Command#CONNECT_TO}.
	 * Attempts to open the specified connection by sending a connection request.
	 * @param connectionID
	 * 		the ID of the connection to open, must be the ID of a ConnectionEndpoint in the ConnectionManager
	 * @return
	 * 		a message describing whether or not execution of the command was successful <br>
	 * 		currently (14.02.2022), it does not describe whether the connection was actually established, just if an attempt was successfully made
	 */
	protected static String handleConnectTo(String connectionID) {
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID);
		
		if (localPoint == null) {
			return "ERROR - There is no connection with the ID \"" + connectionID + "\", so no connection could be started.";
		} else {
			
			Contact contact = QuantumnetworkControllcenter.communicationList.query(connectionID);
			
			if (contact == null) throw new IllegalArgumentException(
					"Attempted to access contact named " + connectionID + " however, no such contact was found. "
					+ "Each connection ID must correspond to the name of a contact in the communication list, because that contacts IP and Port will be used for the connection.");
			
			try {
				localPoint.establishConnection(contact.getIpAddress(), contact.getPort());
				/*
				 * TODO: Really need the better error handling US done some time soon, this is getting ugly...
				 */
				return "Attempted to establish a connection. For now, please look to the system console for feedback.";
				
			} catch (IOException e) {
				return "An I/O Exception occured while trying to establish the connection with ID \"" + connectionID + "\". Message: " + e.getMessage();
			}
		}
		
	}
	
	/**
	 * Handles execution of {@link Command#WAIT_FOR}.
	 * Causes the specified connection to wait for a connection request.
	 * @param connectionID
	 * 		the ID of the connection to wait on, must be the ID of a ConnectionEndpoint in the ConnectionManager
	 * @return
	 * 		a message describing whether or not execution of the command was successful <br>
	 * 		currently (14.02.2022) does not give feedback when a connection is actually established, 
	 * 		just that the specified connection (endpoint) was switched to be waiting for a connection request
	 */
	protected static String handleWaitFor(String connectionID) {
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID);
		
		if (localPoint == null) {
			return "ERROR - Could not start waiting for a connection request on \"" + connectionID + "\" - no such connection endpoint exists.";
		} else {
			/*
			 * TODO: What happens if waitForConnection() is called in a state that is not == CLOSED?
			 * Especially if there already is a connection active, for example, or if the CE is in an Error state.
			 * Is it safe? If not, maybe it should give some kind of feedback? 
			 */
			if (localPoint.reportState() != ConnectionState.CLOSED) {
				return "ERROR - The connection endpoint \"" + connectionID + "\" must be in state " + ConnectionState.CLOSED + " for this command to be safely executed.";
			} else {
				if (localPoint.reportState() == ConnectionState.WAITINGFORCONNECTION) {
					return "ERROR - Endpoint \"" + connectionID + "\" is already waiting for a connection request.";
				} else {
					localPoint.waitForConnection();
					return "Endpoint \"" + connectionID + "\" is now waiting for a connection request.";
				}
				/*
				 * TODO: If possible, give some kind of feedback once a connection has been gotten. 
				 * This would be a bit complicated though, would likely require an EventListener or something similar.
				 */
			}
		}
		
	}

	/**
	 * Handles execution of {@link Command#CONNECTIONS_CLOSE}.
	 * Attempts to close the specified connection.
	 * @param connectionID
	 * 		the ID of the connection to close, must be the ID of a ConnectionEndpoint in the ConnectionManager
	 * @return
	 * 		a message describing whether or not execution of the command was successful
	 */
	public static String handleConnectionsClose(String connectionID) {
		
		ConnectionEndpoint localEndpoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID);
		
		if (localEndpoint == null) {
			return "ERROR - There is no connection with the ID \"" + connectionID + "\", so it could not be closed.";
		} else {
			if (QuantumnetworkControllcenter.conMan.getConnectionState(connectionID) == ConnectionState.CLOSED) {
				return "ERROR - Connection with ID \"" + connectionID + "\" is already closed.";
			} else {
				QuantumnetworkControllcenter.conMan.closeConnection(connectionID);
				return "Attempted to close connection with ID \"" + connectionID  +"\". If any errors occured, they will be printed to the system console. ";
			}
		}
		
	}

	/**
	 * Handles execution of {@link Command#CONNECTIONS_REMOVE}.
	 * Attempts to remove the specified connection.
	 * @param connectionID
	 * 		the ID of the connection to remove, must be the ID of a ConnectionEndpoint in the ConnectionManager
	 * @return
	 * 		a message describing whether or not execution of the command was successful
	 */
	public static String handleConnectionsRemove(String connectionID) {
		
		ConnectionEndpoint localEndpoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID);
		
		if (localEndpoint == null) {
			return "ERROR - There is no connection with the ID \"" + connectionID + "\", so it could not be removed.";
		} else {
			QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint(connectionID);
			return "Closed and removed connection with the ID \"" + connectionID + "\".";
		}
		
	}
	
	/**
	 * Handles {@link Command#HELLO_WORLD}.
	 * Attempts to send a simple (14.02.2022: currently unauthenticated) hello world message along the specified connection.
	 * @param connectionID
	 * 		the ID of the connection to send a hello world along, must be the ID of a ConnectionEndpoint in the ConnectionManager <br>
	 * 		connection must be active (connected) for a message to be sent
	 * @return
	 * 		a message describing whether or not execution of the command was successful
	 */
	protected static String handleHelloWorld(String connectionID) {
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID);
		
		if (localPoint == null) {
			return "ERROR - There is no connection with the ID \"" + connectionID + "\", so no hello world could be sent.";
		}
			
		if (localPoint.reportState() != ConnectionState.CONNECTED) {
			return "ERROR - Can only send messages on connections in state " + ConnectionState.CONNECTED;
		}
		
		MessageSystem.sendMessage(connectionID, TransmissionTypeEnum.TRANSMISSION, "", "Hello World!", "");
		
		return "An attempt has been made to send a hello world message.";
	}
	
}
