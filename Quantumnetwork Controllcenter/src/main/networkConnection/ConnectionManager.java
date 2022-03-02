package networkConnection;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
	private int localPort;
	private ConnectionSwitchbox connectionSwitchbox;
	
	
	/**A ConnectionManager needs to be supplied with the ip under which the local ConnectionEndpoints should be accessible. 
	 * 
	 * @param localAddress the ip address that is being passed on to any local ConnectionEndpoints.
	 */
	public ConnectionManager(String localAddress, int localPort){
		this.localAddress = localAddress;
		this.localPort = localPort;
		connectionSwitchbox = new ConnectionSwitchbox(this.localPort);
		
	}
	
	
	public ConnectionSwitchbox getConnectionSwitchbox() {
		return connectionSwitchbox;
	}
	
	
	 /**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	 * Returns null instead of the new connectionEndpoint if it was not created due to naming and port constraints.
	 * 
	 * This version of the method should be used if the CE is being created because of the local users intentions and not as part of the response to a connection request
	 * from an external source.
	 * 
	 * The CE will attempt to connect to the targetIP and Port as soon as it is created.
	 * 
	 *@param endpointName 	the Identifier for a connection. This Name can be used to access it later
	 *@param targetIP 		the IP of the Server that we wish to connect to.
	 *@param targetPort 	the Port of the Server that we wish to connect to.
	 *@return ConnectionEndpoint	it returns the newly created ConnectionEndpoint. It can also be accessed via ConnectionManger.getConnectionEndpoint().
	 */
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, String targetIP, int targetPort) {
		if(!connections.containsKey(endpointName)) {
			System.out.println("---Received new request for a CE. Creating it now. It will connect to the Server at "+ targetIP +":"+ targetPort +".---");
			connections.put(endpointName, new ConnectionEndpoint(endpointName, targetIP, targetPort));
			return connections.get(endpointName);
		}
		System.err.println("[" + endpointName + "]: Could not create a new ConnectionEndpoint because of the Name- and Portuniqueness constraints.");
		return null;
	}
	
	 /**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	 * Returns null instead of the new connectionEndpoint if it was not created due to naming and port constraints.
	 * 
	 * This version of the method should be used as part of the response to a connection request
	 * from an external source only.
	 * It is called by the local ConnectionEndpointServerHandler and SHOULD NOT BE CALLED FROM ANYWHERE ELSE!
	 * 
	 * The CE will be give In- and OutStreams as well as IP:Port Information about its communication partner.
	 * 
	 * @param endpointName	the Identifier for a connection. This Name can be used to access it later
	 * @param clientSocket	the Socket that was created by accepting the connectionRequest at the ServerSocket.
	 * @param streamOut	the OutputStream that can be used to send messages to the communication partner.
	 * @param streamIn	the InputStream that can be used to receive messages from the communication partner.
	 * @param targetIP	the IP of the communication partner.
	 * @param targetPort	the Port of the communication partner.
	 * @return	it returns the newly created ConnectionEndpoint. It can also be accessed via ConnectionManger.getConnectionEndpoint().
	 */
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, Socket clientSocket, ObjectOutputStream streamOut, ObjectInputStream streamIn, String targetIP, int targetPort) {
		if(!connections.containsKey(endpointName)) {
			connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, clientSocket, streamOut, streamIn, targetIP, targetPort));
			return connections.get(endpointName);
		}
		System.err.println("[" + endpointName + "]: Could not create a new ConnectionEndpoint because of the Name- and Portuniqueness constraints.");
		return null;
	}
	
	
	/**This can be used if the supplied port is being used by any local ConnectionEndpoint.
	 * 
	 * @param portNumber the port to be checked for availability.
	 * @return returns true if the port is being used already by any ConnectionEndpoint, false if the port is free.
	 */
	public boolean isPortInUse(int portNumber) {
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
	public void sendMessage(String connectionID, TransmissionTypeEnum type, String typeArgument, byte[] message, byte[] sig) {	
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
	
	/**
	 * Checks whether the ConnectionManager contains a {@link ConnectionEndpoint} of the given name.
	 * @param connectionName
	 * 		name of the {@link ConnectionEndpoint} to check for
	 * @return
	 * 		true, if the ConnectionManager contains an endpoint of that name <br>
	 * 		false otherwise
	 */
	public boolean hasConnectionEndpoint(String connectionName) {
		return connections.containsKey(connectionName);
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
	
	public int getLocalPort() {
		return localPort;
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
