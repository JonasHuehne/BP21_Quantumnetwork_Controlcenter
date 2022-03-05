package networkConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** This class is used for the creation, destruction and management of multiple {@linkplain ConnectionEndpoint}s. <br>
 * A ConnectionEndpoint represents a connection from our local machine to another machine also running this program. <br>
 * The ConnectionManager allows you to create multiple of these endpoints, allowing connections to different machines,
 * and to manage these connections. It also automatically reacts to incoming connection requests via a {@link ConnectionEndpointServerHandler},
 * creating a new ConnectionEndpoint if a connection request comes in. <br>
 * 
 * (05.03.2022) For testing purposes, it is possible to create multiple ConnectionManagers on one machine to simulate multiple machines running
 * this program. For an example, please see the NetworkTests class.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ConnectionManager {
	
	/** The connections held by this ConnectionManager */
	private Map<String,ConnectionEndpoint> connections = new HashMap<String,ConnectionEndpoint>();
	
	/*
	 * Fields related to handling incoming connection requests.
	 */
	
	/** Our local IP address, to which other ConnectionEndpoints connect */
	private String localAddress;
	/** The port our local server uses to service ConnectionEndpoints connecting to us */
	private int localPort;
	/** This ServerSocket allows other ConnectionEndpoints to connect to us by sending requests to {@link #localAddress}:{@link #localPort}*/
	private ServerSocket masterServerSocket;
	/** This thread continously checks for incoming connection requests */
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	/** Whether this CM is currently accepting ConnectionRequests */
	private boolean isAcceptingConnections = false;
	/** Used for control flow only */
	private boolean submittedTaskOnce = false;
	
	
	/**
	 * Creates and new ConnectionManager.
	 * Automatically begins accepting connection requests.
	 * 
	 * @param localAddress 
	 * 		the ip address that is being passed on to any local ConnectionEndpoints, should be our local IP address
	 * @param localPort
	 * 		the port that this ConnectionManager will be accepting connection requests on, and that contained ConnectionEndpoints will be receiving messages on
	 * @throws IOException 
	 * 		if an I/O Exception occured while trying to open the ServerSocket used for accepting connections
	 */
	public ConnectionManager(String localAddress, int localPort) throws IOException{
		this.localAddress = localAddress;
		this.localPort = localPort;
		
		masterServerSocket = new ServerSocket(this.localPort);
		waitForConnections();
	}
	
	/**
	 * Causes the ConnectionManager to start accepting incoming connection requests.
	 */
	public void waitForConnections() {
		isAcceptingConnections = true;
		
		if (!submittedTaskOnce) { // TODO check if calling waitForConnections() and then stopWaitingForConnections() and then waitForConnections() again works
			// Used to asynchronously wait for incoming connections
			connectionExecutor.submit(() -> {
				while (isAcceptingConnections) {
					Socket clientSocket;
					try { // Wait for a Socket to attempt a connection to our ServerSocket
						clientSocket = masterServerSocket.accept();
					} catch (IOException e) {
						System.err.println("An I/O Exception occured while trying to accept a connection in the ConnectionManager "
								+ "with local IP " + localAddress + " and server port " + localPort + ". Accepting connections is being shut down.");
						System.err.println("Message: " + e.getMessage());
						isAcceptingConnections = false;
						break;
					} // Check that it is from a ConnectionEndpoint of our program
					ConnectionEndpointServerHandler cesh = new ConnectionEndpointServerHandler(clientSocket, localAddress, localPort);
					cesh.run();
					if (cesh.acceptedRequest() == true) { // if it is, and a connection request came in time, add the new CE to our set of CE's
						ConnectionEndpoint ce = cesh.getCE();
						connections.put(ce.getRemoteName(), ce);
						System.out.println("Received a CR from " + ce.getRemoteName() + " and accepted it.");
					} 
				}
			});
			submittedTaskOnce = true;
		}
	}
	
	/**
	 * Causes the ConnectionManager to stop accepting incoming connection requests.
	 */
	public void stopWaitingForConnections() {
		isAcceptingConnections = false;
	}
	
	/**
	 * @return true iff this ConnectionManager is currently accepting incoming connection requests
	 */
	public boolean isWaitingForConnections() {
		return isAcceptingConnections;
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
			connections.put(endpointName, new ConnectionEndpoint(endpointName, targetIP, targetPort, localAddress, localPort));
			return connections.get(endpointName);
		}
		System.err.println("[" + endpointName + "]: Could not create a new ConnectionEndpoint because of the Name- and Portuniqueness constraints.");
		return null;
	}
	
	
	/**This can be used if the supplied port is being used by any local ConnectionEndpoint.
	 * 
	 * @param portNumber the port to be checked for availability.
	 * @return returns true if the port is being used already by any ConnectionEndpoint, false if the port is free.
	 * @deprecated Due to Network rework.
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
	
	/**
	 * @return how many connections are currently managed by this ConnectionManager
	 */
	public int getConnectionsAmount() {
		return connections.size();
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
		localAddress = newLocalAddress;
	}
	
	public void setLocalPort(int newLocalPort) {
		localPort = newLocalPort;
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
