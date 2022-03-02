package networkConnection;

import java.util.*;

/**A class that holds any number of ConnectionEndpoints. These represent connections from one local port to one other port at a give Address. 
 * This class offers methods to create and destroy ConnectionEndpoints, as well as setting and getting their relevant information.
 *
 * @author Jonas Huehne
 *
 */
public class ConnectionManager {
	
	private Map<String,ConnectionEndpoint> connections = new HashMap<String,ConnectionEndpoint>();
	private String localAddress;
	
	
	/**A ConnectionManager needs to be supplied with the ip under which the local ConnectionEndpoints should be accessible. 
	 * 
	 * @param localAddress the ip address that is being passed on to any local ConnectionEndpoints.
	 */
	public ConnectionManager(String localAddress){
		this.localAddress = localAddress;
	}
	
	
	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	 * Returns null instead of the new connectionEndpoint if it was not created duo to naming and port constraints.
	*@param endpointName 	the Identifier for a connection. This Name can be used to access it later
	*@param serverPort 		the local serverPort, this is the port a remote client needs to connect to, since this is where the connectionEndpoint will be listening on.
	*@return ConnectionEndpoint		a representation of a connection line to a different ConnectionEndpoint. Will be stored and accessed from the "Connections" Mapping.
	*/
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, int serverPort) {
		if(!connections.containsKey(endpointName) && !isPortInUse(serverPort)) {
			connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, serverPort));
			return connections.get(endpointName);
		}
		System.err.println("[" + endpointName + "]: Could not create a new ConnectionEndpoint because of the Name- and Portuniqueness constraints.");
		return null;
	}
	
	/**This can be used if the supplied port is being used by any lokal ConnectionEndpoint.
	 * 
	 * @param portNumber the port to be checked for availability.
	 * @return returns true if the port is being used already by any ConnectionEndpoint, false if the port is free.
	 */
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
	
	/**Sends a Message via a local ConnectionEndpoint to the ConnectionEndpoint connected to it.
	 * @param connectionID	the Identifier of the intended connectionEndpoint.
	 * @param type the type of the transmission expressed as a TransmissionTypeEnum
	 * @param typeArgument an additional String that some transmission types use. Can be "" if not needed.
	 * @param sig the optional signature, used by authenticated messages
	 * @param message the String Message that is supposed to be sent via the designated ConnectionEndpoint.
	 */
	public void sendMessage(String connectionID, TransmissionTypeEnum type, String typeArgument, byte[] message, String sig) {	
		connections.get(connectionID).pushMessage(type, typeArgument, message, sig);
	}
	
	/**Returns a ConnectionEndpoint if one by the given name was found. Returns NULL and a Warning otherwise.
	 * 
	 * @param connectionName the Identifier of the intended connectionEndpoint. 
	 * @return ConnectionEndpoint the CE that was requested by ID.
	 */
	public ConnectionEndpoint getConnectionEndpoint(String connectionName) {
		if(connections.containsKey(connectionName)) {
			return connections.get(connectionName);
		}
		System.err.println("Warning: No Connection by the name of " + connectionName + " was found by the ConnectionManager!");
		return null;
	}
	
	/**Returns the ConnectionState of a ConnectionEndpoint given by name.
	 * May Return ConnectionState.ERROR if no CE by the given connectionName was found.
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
			System.err.println("Warning: No active Connection found for Connection Endpoint " + connectionName + ". Aborting closing process!");
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
