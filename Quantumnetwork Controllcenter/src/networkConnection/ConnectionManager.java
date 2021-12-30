package networkConnection;

import java.io.IOException;
import java.util.*;

import MessengerSystem.MessageSystem;

//A class that holds any number of Connections from a local ConnectionEndpoint to another.
public class ConnectionManager {
	
	private Map<String,ConnectionEndpoint> Connections = new HashMap<String,ConnectionEndpoint>();
	private String localAddress;
	
	public ConnectionManager(String localAddress){
		this.localAddress = localAddress;
	}
	
	
	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	*@param EndpointName 	the Identifier for a connection. This Name can be used to access it later
	*@param serverPort 		the local serverPort, this is the port a remote client needs to connect to, since this is where the connectionEndpoint will be listening on.
	*@return ConnectionEndpoint		a representation of a connection line to a different ConnectionEndpoint. Will be stored and accessed from the "Connections" Mapping.
	*/
	public ConnectionEndpoint createNewConnectionEndpoint(String EndpointName, int serverPort) {
		Connections.put(EndpointName, new ConnectionEndpoint(this, EndpointName, localAddress, serverPort));
		if(Connections.size()==1) {
			MessageSystem.setActiveConnection(EndpointName);
		}
		return Connections.get(EndpointName);
	}
	
	
	/**Returns all currently stored connections as a ID<->Endpoint Mapping.
	 * 
	 * @return Map<String,ConnectionEndpoint>	The Mapping containing all existing connectionEndpoints. Can be used to retrieve a CE via its Identifier.
	 */
	public Map<String,ConnectionEndpoint> returnAllConnections(){
		Map<String,ConnectionEndpoint> returnConnections = new HashMap<String,ConnectionEndpoint>();
		//TODO: return new copy, not original!
		return Connections;
	}
	
	/**Sends a Message via a ConnectionEndpoint
	 * @param connectionID	the Identifier of the intended connectionEndpoint.
	 * @param message		the String Message that is supposed to be sent via the designated Endpoint.
	 */
	public void sendMessage(String connectionID,String message) {
		ConnectionEndpoint activeConnectionEndpoint = Connections.get(connectionID);
		activeConnectionEndpoint.pushMessage(message);
	}
	
	
	/**Returns a ConnectionEndpoint if one of the given name was found. Returns NULL and a Warning otherwise.
	 * 
	 * @param ConnectionName the Identifier of the intended connectionEndpoint. 
	 * @return ConnectionEndpoint the cE that was requested by ID.
	 */
	public ConnectionEndpoint getConnectionEndpoint(String ConnectionName) {
		if(Connections.containsKey(ConnectionName)) {
			return Connections.get(ConnectionName);
		}
		System.out.println("Warning: No Connection by the name of " + ConnectionName + " was found by the ConnectionManager!");
		return null;
	}
	
	/**Returns the ConnectionState of a ConnectionEndpoint given by name.
	 * 
	 * @param ConnectionName	the Identifier of the intended connectionEndpoint.
	 * @return ConnectionState	the State of the ConnectionEndpoint expressed as aConnectionStateEnum.
	 */
	public ConnectionState getConnectionState(String ConnectionName) {
		ConnectionEndpoint ce = getConnectionEndpoint(ConnectionName);
		if(ce != null) {
			return ce.reportState();
		}
		return ConnectionState.ERROR;
	}
	
	/**Changes the current LocalAddress to a new Value. This resets all active connectionEndpoints and closes their connections.
	 *
	 * @param newLocalAddress the new LocalAddress
	 */
	public void setLocalAddress(String newLocalAddress) {
		System.out.println("[ConnectionManager]: Updating LocalAddress from " + localAddress + " to " + newLocalAddress + "!");
		localAddress = newLocalAddress;
		Connections.forEach((k,v) -> {
			try {
				Connections.get(k).closeConnection();
			} catch (IOException e) {
				System.out.println("ERROR: while updating the localAddress, the ConnectionEndpoint " + k + " did not close its connection properly!");
				e.printStackTrace();
			}
		});
		Connections.forEach((k,v) -> Connections.get(k).updateLocalAddress(localAddress));
	}
	
	/**Returns the localAddress Value currently in use by all connectionEndpoints.
	 * 
	 * @return the current localAddress
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**Closes a named connection if existing and open. Does not destroy the connectionEndpoint, use destroyConnectionEndpoint for that.
	 * 
	 * @param ConnectionName	the Identifier of the intended connectionEndpoint.
	 */
	public void closeConnection(String ConnectionName) {
		if(getConnectionEndpoint(ConnectionName) != null && getConnectionState(ConnectionName) == ConnectionState.Connected) {
			try {
				getConnectionEndpoint(ConnectionName).pushMessage("termconn:::");
				getConnectionEndpoint(ConnectionName).closeConnection();
			} catch (IOException e) {
				System.out.println("A problem occured while closing down the connection of " + ConnectionName + "!");
				e.printStackTrace();
			}
		}else {
			System.out.println("Warning: No active Connection found for Connection Endpoint " + ConnectionName + ". Aborting closing process!");
		}
	}
	
	/**Closes all connections if existing and open. Does not destroy the connectionEndpoints, use destroyAllConnectionEndpoints for that.
	 * 
	 */
	public void closeAllConnections() {
		Connections.forEach((k,v) -> closeConnection(k));
	}

	/**Completely destroys a given connectionEndpoint after shutting down its potentially active connection.
	 * 
	 * @param connectionID the ID of the connectionEndpoint that is no longer needed.
	 */
	public void destroyConnectionEndpoint(String connectionID) {
		try {
			System.out.println("[ConnectionManager]: Destroying ConnectionEndpoint " + connectionID);
			Connections.get(connectionID).closeConnection();
			Connections.remove(connectionID);
		} catch (IOException e) {
			System.out.println("Error while closing down Connection " + connectionID + " as part of the ConnectionEndpoints destruction.");
			e.printStackTrace();
		}
	}
	
	/**Completely destroys all connectionEndpoints after shutting down their potentially active connections.
	 * 
	 */
	public void destroyAllConnectionEndpoints() {
		Connections.forEach((k,v) -> {
			try {
				Connections.get(k).closeConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			});
		Connections.clear();
	}
}
