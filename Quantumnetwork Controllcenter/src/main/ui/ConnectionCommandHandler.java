package ui;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import communicationList.CommunicationList;
import communicationList.Contact;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;

public class ConnectionCommandHandler {
	
	static CommunicationList communicationList = QuantumnetworkControllcenter.communicationList;
	
	/** When establishing a new connection, this is the default port this */
	public static final int DEFAULT_PORT = 17144;
	
	/*
	 * Regarding the name of ConnectionEndpoints:
	 * For this implementation I have chosen to name them after the contact they are connecting to.
	 */
	
	protected static String handle_connectionsAdd(String[] commandArgs) {
		
		String contactName = "";
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
			// Not throwing an illegal argument exception here because this is more likely caused by bad input than progammatic mistakes
		}
		
		// Make sure given contact exists
		Contact contact = QuantumnetworkControllcenter.communicationList.query(contactName);
		if (contact == null) {
			return "ERROR - Can not create a connection to contact \"" + contactName + "\" - no such contact could be found in the communication list.";
		} 
		
		// Don't connect to the same contact twice
		/*
		 *  TODO: Theoretically this is already checked in conMan.createNewConnectionEndpoint(...)
		 *  however, currently (27.01.2022) the method outputs either null if a CE of that name already exists
		 *  or if there is already a CE in the CM listening on the given local port. Just by receiving null
		 *  the caller can't tell which case it is though, so we check seperately here to give clear error messages.
		 */
		ConnectionEndpoint previousCE = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(contactName);
		if (previousCE != null) {
			return "ERROR - Can not create a connection to contact \"" + contactName + "\" - such a connection already exists.";
		} 
		
		// If contact exists, and there is no connection, add the new connection
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(contactName, localPort);
		
		// (27.01.2022, see previous TODO) localPoint will only be null if port is in use
		
		if (localPoint == null) {
			return "ERROR - Could not create the specified connection. The port " + localPort + " is already being listened on by another connection.";
			/* 
			 * TODO: Currently, even two inactive connections can't have the same local port.
			 * This could be improved 
			 */
		}
		
		return "Successfully created connection to contact \"" + contactName + "\", listening on port " + localPort + ". "
				+ "The connection is still inactive. Use ... to open the connection."; // TODO
	}
	
	protected static String handle_ConnectionShow() {
		
		Map<String, ConnectionEndpoint> endpoints = QuantumnetworkControllcenter.conMan.returnAllConnections();
		
		if (endpoints.isEmpty()) {
			return "This machine currently has no connections.";
		}
		
		StringBuilder output = new StringBuilder();
		
		// Used to build a table (TODO: Make this prettier)
		String format = "[ %s , %s , %s , %s , %s ]";
		String header = String.format(format, "Connection ID", "IP", "Port (Remote)", "Port (Local)", "Connection Status");
		
		output.append(header);
		output.append(System.lineSeparator());
		
		// For each Endpoint add an entry to the table
		for (Map.Entry<String, ConnectionEndpoint> endpoint : endpoints.entrySet()) {
			String connectionID = endpoint.getKey();
			String connectionState = endpoint.getValue().reportState().toString();
			int localPort = endpoint.getValue().getServerPort();
			
			/* 
			 * Intuitively, one might try endpoint.getValue.getRemoteAddress() and endpoint.getValue.getRemoteIP() here,
			 * however, currently (28.01.2022) these values are only set by the method ConnectionEndpoint.establishConnection(..),
			 * that is, they are only set if the CE tries to be the party that opens a connection.
			 * So, instead we get the contact associated with this connection* 
			 * and get the remote IP and port from there, since that is also what we (will) do in handleConnectTo(..).
			 * 
			 * *(no connection exists without a contact, contact name == connection ID)
			 * TODO / See Issue #39
			 */
			Contact contact = communicationList.query(connectionID);
			
			if (contact == null) throw new NullPointerException(
					"Attempted to access contact named " + connectionID + " however, no such contact was found. "
					+ "Each connection ID must correspond to the name of a contact in the communication list, because that contacts IP and Port will be used for the connection.");
			
			String remoteIP = contact.getIpAddress();
			int remotePort = contact.getPort();		
			
			output.append(String.format(format, connectionID, remoteIP, remotePort, localPort, connectionState));
			// TODO: TEMPORARY - FOR DEBUGGING ONLY:
			output.append("Message Stack Size: " + endpoint.getValue().sizeOfMessageStack());
			output.append(System.lineSeparator());
		}
		
		// Temporary?
		String info = 
					"Connection ID: The name of this connection endpoint. This is also the name of the contact this will be a connection to, once established. " + System.lineSeparator()
				+ 	"IP: The IP that outgoing messages will be sent to" + System.lineSeparator()
				+	"Port (Remote): The port that outgoing messages and connection requests will be sent to. " + System.lineSeparator()
				+ 	"Port (Local): The port that this connection will wait for messages or connection requests on, depending on state. " + System.lineSeparator()
				+	"Connection Status: Information on the status of this connection, such as whether it is currently active or not.";
		output.append(info);
		
		return output.toString();
	}
	
	protected static String handle_connectTo(String contactName) {
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(contactName);
		
		if (localPoint == null) {
			return "ERROR - There is no connection with the ID \"" + contactName + "\", so no connection could be started.";
		} else {
			
			Contact contact = QuantumnetworkControllcenter.communicationList.query(contactName);
			
			if (contact == null) throw new NullPointerException(
					"Attempted to access contact named " + contactName + " however, no such contact was found. "
					+ "Each connection ID must correspond to the name of a contact in the communication list, because that contacts IP and Port will be used for the connection.");
			
			try {
				localPoint.establishConnection(contact.getIpAddress(), contact.getPort());
				
				/*
				 * TODO: Really need the better error handling US done some time soon, this is getting ugly...
				 */
				return "Attempted to establish a connection. For now, please look to the system console for feedback.";
				
			} catch (IOException e) {
				return "An I/O Exception occured while trying to establish the connection with ID \"" + contactName + "\". Message: " + e.getMessage();
			}
		}
		
	}
	
	protected static String handle_waitFor(String contactName) {
		
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(contactName);
		
		if (localPoint == null) {
			return "ERROR - Could not start waiting for a connection request on \"" + contactName + "\" - no such connection endpoint exists.";
		} else {
			/*
			 * TODO: What happens if waitForConnection() is called in a state that is not == CLOSED?
			 * Especially if there already is a connection active, for example, or if the CE is in an Error state.
			 * Is it safe? If not, maybe it should give some kind of feedback? 
			 */
			if (localPoint.reportState() != ConnectionState.CLOSED) {
				return "ERROR - The connection endpoint \"" + contactName + "\" must be in state " + ConnectionState.CLOSED.toString() + " for this command to be safely executed.";
			} else {
				localPoint.waitForConnection();
				return "Endpoint \"" + contactName + "\" is now waiting for a connection request.";
				/*
				 * TODO: If possible, give some kind of feedback once a connection has been gotten. 
				 * This would be a bit complicated though, would likely require an EventListener or something similar.
				 */
			}
		}
		
	}
	
	protected static String handle_helloWorld(String contactName) {
	
		ConnectionEndpoint localPoint = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(contactName);
		
		if (localPoint == null) {
			return "ERROR - There is no connection with the ID \"" + contactName + "\", so no hello world could be sent.";
		}
			
		if (localPoint.reportState() != ConnectionState.CONNECTED) {
			return "ERROR - Can only send messages on connections in state " + ConnectionState.CONNECTED;
		}
		
		// TODO adjust this
		// localPoint.pushMessage("msg", "Hello World!");
		
		return "An attempt has been made to send a hello world message.";
	}
	
}
