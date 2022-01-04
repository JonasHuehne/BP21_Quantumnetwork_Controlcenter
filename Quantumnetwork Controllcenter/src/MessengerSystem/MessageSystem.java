package MessengerSystem;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things.
 * Select active connectionEndpoint and start sending and receiving messages!
 * The first connectionEndpoint beeing created is automatically selected as active.
 * 
 * @author Jonas H�hne
 *
 */
public class MessageSystem {
	
public static ConnectionManager conMan;
private static String activeConnection = null;


/**This is used to set the active connectionEndpoint. Most Methods of this class operate in relation to that cE.
 * F.ex. sendMessage() sends a Message on that cEs Connection etc.
 * 
 * @param newActiveConnectionID the ID of the new active connectionEndpoint.
 */
public static void setActiveConnection(String newActiveConnectionID) {
	System.out.println("Active ConnectionEndpoint is: " + newActiveConnectionID);
	activeConnection = newActiveConnectionID;
}

/**Can be used to find out what connectionEndpoint is currently in use.
 * 
 * @return the ID of the currently active connectionEndpoint
 */
public static String getActiveConnection() {
	return activeConnection;
}
	
/**This sends a message on the currently active connection. No confirmation is expected from the recipient.
 * 
 * @param message the message to be sent on the active connection
 */
public static void sendMessage(String message) {
	if(conMan == null) {
		System.out.println("WARNING: Tried to send a message via the MessageSystem before initializing the QuantumnetworkControllcenter, thereby setting the connectionManager Reference.");
		return;
	}
	System.out.println("[" + activeConnection + "]: Sending Message: " + message);
	ConnectionState state = conMan.getConnectionState(activeConnection);
	if(state == ConnectionState.CONNECTED) {
		conMan.sendMessage(activeConnection, "msg:::" + message);
	}
	else {
		System.out.println("[" + activeConnection + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
	}
}


/**This sends a message and the recipient is going to echo the message back to us.
 * 
 * @param message the message to be sent on the active connection
 * @return returns True if the confirmation of the message has been received, False if it times out.
 */
public static boolean sendConfirmedMessage(String message) {
	System.out.println("[" + activeConnection + "]: Sending Confirm-Message: " + message);
	ConnectionState state = QuantumnetworkControllcenter.conMan.getConnectionState(activeConnection);
	if(state == ConnectionState.CONNECTED) {
		//Send message
		QuantumnetworkControllcenter.conMan.sendMessage(activeConnection, "confirm:::" + message);
		boolean waitForConfirmation = true;
		Instant startWait = Instant.now();
		Instant current;
		//Wait for confirmation
		System.out.println("[" + activeConnection + "]: Starting to wait for Message Confirmation!");
		while(waitForConfirmation) {
			current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() <= 10 && QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).getConfirmations().contains(message)) {
				waitForConfirmation = false;
				System.out.println("[" + activeConnection + "]: Message Confirmation received!");
				QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).clearConfirmation(message);
				return true;
			}
			else if (Duration.between(startWait, current).toSeconds() > 10) {
				waitForConfirmation = false;
				return false;
			}
		}
		return false;
	}
	else {
		System.out.println("[" + activeConnection + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
	}
	return false;

}
	
/**reads the oldest Message available and removes it from the stack(actually a queue)
 * 
 * @return the oldest message
 */
public static String readReceivedMessage() {
	return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).readMessageFromStack();
}

/**returns the oldest message, but does not remove it from the queue
 * 
 * @return the oldest message that was received and not yet read(removed)
 */
public static String previewReceivedMessage() {
	return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).peekMessageFromStack();
}

/**returns the last message that was received but does not remove it from the queue
 * 
 * @return the latest message
 */
public static String previewLastReceivedMessage() {
	return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).peekLatestMessageFromStack();
}

/**Returns the number of messages that are on the stack and waiting to be read(removed).
 * 
 * @return the number of messages.
 */
public static int getNumberOfPendingMessages() {
	return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).sizeOfMessageStack();
}

/**Returns a linkedList of all un-read messages
 * 
 * @return the list of unread messages.
 */
public static LinkedList<String> getAllReceivedMessages(){
	return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(activeConnection).getMessageStack();
}
	
	
	
	
	
	
	
	
	
	
	
	

}