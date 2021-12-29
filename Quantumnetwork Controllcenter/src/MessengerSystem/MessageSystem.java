package MessengerSystem;

import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionState;

public class MessageSystem {
	
	
private static String activeConnection = null;


public static void setActiveConnection(String newActiveConnectionID) {
	System.out.println("Active ConnectionEndpoint is: " + newActiveConnectionID);
	activeConnection = newActiveConnectionID;
}

public static String getActiveConnection() {
	return activeConnection;
}
	
public static void sendConfirmedMessage(String message) {
	System.out.println("[" + activeConnection + "]: Sending Confirm-Message: " + message);
	ConnectionState state = QuantumnetworkControllcenter.conMan.getConnectionState(activeConnection);
	if(state == ConnectionState.Connected) {
		QuantumnetworkControllcenter.conMan.sendMessage(activeConnection, "confirm:::" + message);
	}
	else {
		System.out.println("[" + activeConnection + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
	}

}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
