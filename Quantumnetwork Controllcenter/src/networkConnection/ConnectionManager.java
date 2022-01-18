package networkConnection;

import java.util.*;




/**A class that holds any number of Connections from a local ConnectionEndpoint to another.
 * 
 * @author Jonas Hühne
 *
 */
public class ConnectionManager {
	
	private Map<String,ConnectionEndpoint> connections = new HashMap<String,ConnectionEndpoint>();
	private String localAddress;
	
	public ConnectionManager(String localAddress){
		this.localAddress = localAddress;
	}
	
	
	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	*@param endpointName 	the Identifier for a connection. This Name can be used to access it later
	*@param serverPort 		the local serverPort, this is the port a remote client needs to connect to, since this is where the connectionEndpoint will be listening on.
	*@return ConnectionEndpoint		a representation of a connection line to a different ConnectionEndpoint. Will be stored and accessed from the "Connections" Mapping.
	*/
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, int serverPort) {
		if(!connections.containsKey(endpointName) && !isPortInUse(serverPort)) {
			connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, serverPort));
			return connections.get(endpointName);
		}
		
		return null;
	}
	
	private boolean isPortInUse(int portNumber) {
		boolean isInUse = false;
		for (ConnectionEndpoint v : connections.values()) {
			if(v.getServerPort() == portNumber) {
				isInUse = true;
			}
		}
		return isInUse;
	}
	
	
	/**Returns all currently stored connections as a ID<->Endpoint Mapping.
	 * 
	 * @return Map<String,ConnectionEndpoint>	The Mapping containing all existing connectionEndpoints. Can be used to retrieve a CE via its Identifier.
	 */
	public Map<String,ConnectionEndpoint> returnAllConnections(){
		Map<String,ConnectionEndpoint> returnConnections = new HashMap<String,ConnectionEndpoint>();
		returnConnections.putAll(connections);
		return returnConnections;
	}
	
	/**Sends a Message via a ConnectionEndpoint
	 * @param connectionID	the Identifier of the intended connectionEndpoint.
	 * @param message		the String Message that is supposed to be sent via the designated Endpoint.
	 */
	public void sendMessage(String connectionID, String type, String message) {
		ConnectionEndpoint activeConnectionEndpoint = connections.get(connectionID);
		activeConnectionEndpoint.pushMessage(type, message);
	}
	
	
	/**Returns a ConnectionEndpoint if one of the given name was found. Returns NULL and a Warning otherwise.
	 * 
	 * @param connectionName the Identifier of the intended connectionEndpoint. 
	 * @return ConnectionEndpoint the cE that was requested by ID.
	 */
	public ConnectionEndpoint getConnectionEndpoint(String connectionName) {
		if(connections.containsKey(connectionName)) {
			return connections.get(connectionName);
		}
		System.out.println("Warning: No Connection by the name of " + connectionName + " was found by the ConnectionManager!");
		return null;
	}
	
	/**Returns the ConnectionState of a ConnectionEndpoint given by name.
	 * 
	 * @param connectionName	the Identifier of the intended connectionEndpoint.
	 * @return ConnectionState	the State of the ConnectionEndpoint expressed as aConnectionStateEnum.
	 */
	public ConnectionState getConnectionState(String connectionName) {
		ConnectionEndpoint ce = getConnectionEndpoint(connectionName);
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
		connections.forEach((k,v) -> {
		connections.get(k).closeConnection(true);
		});
		connections.forEach((k,v) -> connections.get(k).updateLocalAddress(localAddress));
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
	 * @param connectionName	the Identifier of the intended connectionEndpoint.
	 */
	public void closeConnection(String connectionName) {
		if(getConnectionEndpoint(connectionName) != null && getConnectionState(connectionName) == ConnectionState.CONNECTED) {
			getConnectionEndpoint(connectionName).closeConnection(true);
		}else {
			System.out.println("Warning: No active Connection found for Connection Endpoint " + connectionName + ". Aborting closing process!");
		}
	}
	
	/**Closes all connections if existing and open. Does not destroy the connectionEndpoints, use destroyAllConnectionEndpoints for that.
	 * 
	 */
	public void closeAllConnections() {
		connections.forEach((k,v) -> closeConnection(k));
	}

	/**Completely destroys a given connectionEndpoint after shutting down its potentially active connection.
	 * 
	 * @param connectionID the ID of the connectionEndpoint that is no longer needed.
	 */
	public void destroyConnectionEndpoint(String connectionID) {
		System.out.println("[ConnectionManager]: Destroying ConnectionEndpoint " + connectionID);
		connections.get(connectionID).closeConnection(true);
		connections.remove(connectionID);
	}
	
	/**Completely destroys all connectionEndpoints after shutting down their potentially active connections.
	 * 
	 */
	public void destroyAllConnectionEndpoints() {
		connections.forEach((k,v) -> {
			connections.get(k).closeConnection(true);
			});
		connections.clear();
	}
}
