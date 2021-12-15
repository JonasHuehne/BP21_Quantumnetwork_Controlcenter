package networkConnection;

import java.io.IOException;
import java.util.*;

//A class that holds any number of Connections from a local ConnectionEndpoint to another.
public class ConnectionManager {
	
	private Map<String,ConnectionEndpoint> Connections = new HashMap<String,ConnectionEndpoint>();
	private String localAddress;
	
	public ConnectionManager(String localAddress){
		this.localAddress = localAddress;
	}
	
	
	//Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	public ConnectionEndpoint createNewConnectionEndpoint(String EndpointName, int serverPort) {
		Connections.put(EndpointName, new ConnectionEndpoint(this, EndpointName, localAddress, serverPort));
		return Connections.get(EndpointName);
	}
	
	
	//Returns all currently stored connections as a ID<->Endpoint Mapping.
	public Map<String,ConnectionEndpoint> returnAllConnections(){
		return Connections;
	}
	
	
	public void sendMessage(String connectionID,String message) {
		ConnectionEndpoint activeConnectionEndpoint = Connections.get(connectionID);
		activeConnectionEndpoint.pushMessage(message);
	}
	
	
	//Returns a ConnectionEndpoint if one of the given name was found. Returns NULL and a Warning otherwise.
	public ConnectionEndpoint getConnectionEndpoint(String ConnectionName) {
		if(Connections.containsKey(ConnectionName)) {
			return Connections.get(ConnectionName);
		}
		System.out.println("Warning: No Connection by the name of " + ConnectionName + " was found by the ConnectionManager!");
		return null;
	}
	
	//Returns the ConnectionState of a ConnectionEndpoint given by name.
	public ConnectionState getConnectionState(String ConnectionName) {
		ConnectionEndpoint ce = getConnectionEndpoint(ConnectionName);
		return ce.reportState();
	}
	
	//Closes a named connection if existing and open.
	public void closeConnection(String ConnectionName) {
		if(getConnectionState(ConnectionName) == ConnectionState.Connected) {
			try {
				getConnectionEndpoint(ConnectionName).pushMessage("TerminateConnection:");
				getConnectionEndpoint(ConnectionName).closeConnection();
			} catch (IOException e) {
				System.out.println("A problem occured while closing down the connection of " + ConnectionName + "!");
				e.printStackTrace();
			}
		}
	}

}
