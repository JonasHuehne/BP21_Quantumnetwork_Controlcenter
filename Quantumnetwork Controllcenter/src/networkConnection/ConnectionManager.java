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
	
	
	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	*@param EndpointName 	the Identifier for a connection. This Name can be used to access it later
	*@param serverPort 		the local serverPort, this is the port a remote client needs to connect to, since this is where the connectionEndpoint will be listening on.
	*@return ConnectionEndpoint		a representation of a connection line to a different ConnectionEndpoint. Will be stored and accessed from the "Connections" Mapping.
	*/
	public ConnectionEndpoint createNewConnectionEndpoint(String EndpointName, int serverPort) {
		Connections.put(EndpointName, new ConnectionEndpoint(this, EndpointName, localAddress, serverPort));
		return Connections.get(EndpointName);
	}
	
	
	/**Returns all currently stored connections as a ID<->Endpoint Mapping.
	 * 
	 * @return Map<String,ConnectionEndpoint>	The Mapping containing all existing connectionEndpoints. Can be used to retrieve a CE via its Identifier.
	 */
	public Map<String,ConnectionEndpoint> returnAllConnections(){
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
		return ce.reportState();
	}
	
	/**Closes a named connection if existing and open.
	 * 
	 * @param ConnectionName	the Identifier of the intended connectionEndpoint.
	 */
	public void closeConnection(String ConnectionName) {
		if(getConnectionEndpoint(ConnectionName) != null && getConnectionState(ConnectionName) == ConnectionState.Connected) {
			try {
				getConnectionEndpoint(ConnectionName).pushMessage("TerminateConnection:");
				getConnectionEndpoint(ConnectionName).closeConnection();
			} catch (IOException e) {
				System.out.println("A problem occured while closing down the connection of " + ConnectionName + "!");
				e.printStackTrace();
			}
		}else {
			System.out.println("Warning: No active Connection found for Connection Endpoint " + ConnectionName + ". Aborting closing process!");
		}
	}

}
