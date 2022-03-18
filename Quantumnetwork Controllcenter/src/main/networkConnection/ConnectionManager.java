package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import communicationList.CommunicationList;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.PortIsInUseException;
import frame.QuantumnetworkControllcenter;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

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

	/** Logger for this ConnectionManager */
	static Log conManLog = new Log("ConnectionManager Log", LogSensitivity.WARNING);
	
	/** Ports in use by any ConnectionManager */
	private static HashSet<Integer> portsInUse = new HashSet<Integer>();
	
	/** The connections held by this ConnectionManager */
	private Map<String,ConnectionEndpoint> connections = new HashMap<String,ConnectionEndpoint>();
	
	/*
	 * Fields related to handling incoming connection requests.
	 */
	
	/** Our local IP address, to which other ConnectionEndpoints connect */
	private String localAddress;
	/** The port our local server uses to service ConnectionEndpoints connecting to us */
	private int localPort;
	/** The local name that connection endpoints in this ConnectionManager introduce themselves as */
	private String localName;
	/** This ServerSocket allows other ConnectionEndpoints to connect to us by sending requests to {@link #localAddress}:{@link #localPort}*/
	private ServerSocket masterServerSocket;
	/** This thread continuously checks for incoming connections to the master server socket */
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	/** Whether this CM is currently accepting ConnectionRequests */
	private boolean isAcceptingConnections = false;
	/** Used for control flow only */
	private boolean submittedTaskOnce = false;
	
	/** true <==> no two connections to the same IP:Port pairing are allowed (should also make self-connections impossible) <br>
	 *  for actual use we recommend setting this to true, however, it may make some manual tests impossible that involve connecting to oneself */
	private boolean oneConnectionPerIpPortPair = false;
	
	/** When answering an incoming connection request, this communication list will be check if it contains
	 *  an entry for that connection's IP:Port pair. If it does, we can name the connection based on that
	 *  entry and also set the public key for the connection. May be null. */
	private CommunicationList commList;

	/**
	 * Creates and new ConnectionManager.
	 * Automatically begins accepting connection requests.
	 * 
	 * @param localAddress 
	 * 		the ip address that is being passed on to any local ConnectionEndpoints, should be our local IP address
	 * @param localPort
	 * 		the port that this ConnectionManager will be accepting connection requests on, and that contained ConnectionEndpoints will be receiving messages on <br>
	 * 		must not be in use by any other ConnectionManager
	 * @param localName
	 * 		local name that will be passed on to any ConnectionEndpoints in the manager, 
	 * 		should be the name of this machine / the name you wish to have in the network
	 * @param commlist
	 * 		when answering an incoming connection request, this communication list will be check if it contains
	 *  	an entry for that connection's IP:Port pair. If it does, we can name the connection based on that
	 *  	entry and also set the public key for the connection. May be null.
	 * @throws IOException 
	 * 		if an I/O Exception occurred while trying to open the ServerSocket used for accepting connections
	 * @throws PortIsInUseException
	 * 		if the specified port is already in use by another ConnectionManager <br>
	 * 		ports used by a ConnectionManager remain marked as used until the program is restarted
	 */
	public ConnectionManager(String localAddress, int localPort, String localName, CommunicationList commlist) throws IOException, PortIsInUseException{
		if (portsInUse.contains(localPort)) throw new PortIsInUseException("Port " + localPort + " is already in use by a ConnectionManager.");
		
		this.localAddress = localAddress;
		this.localPort = localPort;
		this.localName = localName;
		this.commList = commlist;
		
		masterServerSocket = new ServerSocket(this.localPort);
		portsInUse.add(localPort);
		conManLog.logInfo("Created new ConnectionManager \"" + localName + "\" with localPort " + localPort + " at IP " + localAddress);
		waitForConnections();
	}
	
	/**
	 * Causes the ConnectionManager to start accepting incoming connection requests. <br>
	 * Does nothing if the connection manager is already waiting for connection requests. <br>
	 * 
	 * @implNote At some points in this implementation, I/O exceptions may occur 
	 * (particularly, with the ServerSocket.accept() methods and the handling of the client sockets InputStreams).
	 * If this happens, the ConnectionManager stops waiting for connections, and the Exception that occurred is logged.
	 */
	public final void waitForConnections() {
		isAcceptingConnections = true;

		if (!submittedTaskOnce) {
			// Used to asynchronously wait for incoming connections
			connectionExecutor.submit(() -> {
				while (isAcceptingConnections) {
					Socket clientSocket;
					try { // Wait for a Socket to attempt a connection to our ServerSocket
						conManLog.logInfo("[CM " + localName + "(" + localPort +")] Now accepting connections on the master server socket with port " + localPort);
						clientSocket = masterServerSocket.accept();
						conManLog.logInfo("[CM " + localName + "(" + localPort +")] Accepted a request on master server socket and created new local client socket.");
					} catch (IOException e) {
						conManLog.logWarning("[CM " + localName + "(" + localPort +")] An I/O Exception occurred while trying to accept a connection in the ConnectionManager "
								+ "with local IP " + localAddress + " and server port " + localPort + ". Accepting connections is being shut down." , e);
						isAcceptingConnections = false;
						break;
					} // Check that it is from a ConnectionEndpoint of our program
					
					ConnectionEndpointServerHandler cesh; 
					try { // Construct a CESH for the socket that just connected to our server socket
						cesh = new ConnectionEndpointServerHandler(clientSocket, this);
						conManLog.logInfo("[CM " + localName + "(" + localPort +")] Created CESH for newly received client socket.");
						cesh.run();
					} catch (IOException e) {
						conManLog.logWarning("[CM " + localName + "(" + localPort +")] An I/O Exception occurred while trying to construct the ConnectionEndpointServerHandler "
								+ "with local IP " + localAddress + " and server port " + localPort + ". Accepting connections is being shut down.", e);
						isAcceptingConnections = false;
						break;
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
		conManLog.logInfo("[CM " + localName + "(" + localPort +")] No longer accepting incoming connection requests.");
		isAcceptingConnections = false;
	}
	
	/**
	 * @return true iff this ConnectionManager is currently accepting incoming connection requests
	 */
	public boolean isWaitingForConnections() {
		return isAcceptingConnections;
	}

	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	 *
	 * This version of the method should be used if the CE is being created because of
	 * a local users actions, and <i>not</i> in response to an external connection request.
	 *
	 * The CE will attempt to connect to the targetIP and Port as soon as it is created.
	 *
	 *	@param endpointName
	 *		the identifier for a connection. This name can be used to access it later
	 *	@param targetIP
	 *		IP of the {@linkplain ConnectionEndpoint} that the newly created CE should connect to
	 *	@param targetPort
	 *		server port of the {@linkplain ConnectionEndpoint} that the newly created CE should connect to
	 *  @param pk
	 *  	public key used to verify messages received from the newly created endpoint <br>
	 *  	may be "" or null if not needed or unknown
	 *	@return ConnectionEndpoint
	 *		the newly created {@linkplain ConnectionEndpoint} <br>
	 *		can also be accessed via {@linkplain #getConnectionEndpoint(String)} with argument {@code endpointName}
	 * 	@throws ConnectionAlreadyExistsException
	 * 		if a connection with the specified name is already managed by this ConnectionManager
	 * @throws IpAndPortAlreadyInUseException
	 * 		if a connection with the same IP and Port pairing is already in this ConnectionManager
	 */
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, String targetIP, int targetPort, String pk)
		throws ConnectionAlreadyExistsException, IpAndPortAlreadyInUseException {
		if(!connections.containsKey(endpointName)) {
			// no two connections to the same IP / Port pairing
			//if (oneConnectionPerIpPortPair && !ipAndPortAreFree(targetIP, targetPort)) throw new IpAndPortAlreadyInUseException(targetIP, targetPort);
			conManLog.logInfo("[CM " + localName + " (" + localPort + ")] Received local request to create a CE with ID " + endpointName + ". "
					+ "CE will attempt to connect to " + targetIP + ":" + targetPort);
			ConnectionEndpoint ce = new ConnectionEndpoint(endpointName, targetIP, targetPort, getLocalAddress(), getLocalPort(), localName, pk);
			connections.put(endpointName, ce);
			return ce;
		} else {
			ConnectionAlreadyExistsException e = new ConnectionAlreadyExistsException(endpointName);
			conManLog.logWarning("[CM " + localName + " (" + localPort + ")] Received a local request to create a CE with ID " + endpointName + " but failed to create it.", e);
			throw e;
		}
	}

	/**Creates a new ConnectionEndpoint and stores the Connection-Name and Endpoint-Ref.
	 * 
	 * This version of the method should be used if the CE is being created as part of the response to a connection request from an external source.
	 * 
	 * The CE is given an already connected Socket and In-/Output Streams.
	 * 
	 *	@param endpointName 	
	 *		the identifier for a connection. This name can be used to access it later
	 *	@param clientSocket
	 *		the Socket that was created for this CE when the external Client connected to the local Server. It was created by the {@linkplain ConnectionEndpointServerHandler}.
	 *	@param streamOut
	 *		the OutStream for this CE. It was created by the {@linkplain ConnectionEndpointServerHandler} and will be used to send messages.
	 *	@param streamIn
	 *		the InStream for this CE. It was created by the {@linkplain ConnectionEndpointServerHandler} and will be used to receive messages.
	 *	@param targetIP 
	 *		IP of the {@linkplain ConnectionEndpoint} that the newly created CE is connect to
	 *	@param targetPort 	
	 *		server port of the {@linkplain ConnectionEndpoint} that the newly created CE is connect to
	 *	@return ConnectionEndpoint
	 *		the newly created {@linkplain ConnectionEndpoint} <br>
	 *		can also be accessed via {@linkplain #getConnectionEndpoint(String)} with argument {@code endpointName}
	 * 	@throws ConnectionAlreadyExistsException 
	 * 		if a connection with the specified name is already managed by this ConnectionManager
	 * @throws IpAndPortAlreadyInUseException 
	 * 		if a connection with the same IP and Port pairing is already in this ConnectionManager
	 */
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, Socket clientSocket, ObjectOutputStream streamOut, ObjectInputStream streamIn, String targetIP, int targetPort) 
			throws ConnectionAlreadyExistsException, IpAndPortAlreadyInUseException {
			if(!connections.containsKey(endpointName)) {
				// no two connections to the same IP / Port pairing
				//if (oneConnectionPerIpPortPair) && !ipAndPortAreFree(targetIP, targetPort)) //Commented out, because now IP and Port can be reused!
				//	throw new IpAndPortAlreadyInUseException(targetIP, targetPort);
				conManLog.logInfo("[CM " + localName + " (" + localPort + ")] Received external request (presumably from CESH) to create a CE with ID " + endpointName + ". "
						+ "CE will attempt to connect to " + targetIP + ":" + targetPort);
				ConnectionEndpoint ce 
				= new ConnectionEndpoint(endpointName, getLocalAddress(), clientSocket, streamOut, streamIn, targetIP, targetPort, getLocalPort(), localName);
				connections.put(endpointName, ce);
				return ce;
			} else {
				ConnectionAlreadyExistsException e = new ConnectionAlreadyExistsException(endpointName);
				conManLog.logWarning("[CM " + localName + " (" + localPort + ")] Received a connection request to create endpoint with ID " + endpointName + " but failed.", e);
				throw e;
			}
		}

	/**@deprecated Not necessary anymore, since now many connections can be run via the same IP:Port.
	 * Utility method. Used to check if an IP/Port pairing is not used by any connection in the manager at the moment.
	 */
	private boolean ipAndPortAreFree(String ip, int port) {
		for (ConnectionEndpoint ce : connections.values()) {
			if (ce.getRemoteAddress().equals(ip) && ce.getRemotePort() == port) return false;
		}
		return true;
	}
	
	/**
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
	 * @param connectionID	the identifier of the intended connectionEndpoint.
	 * @param message the message that is supposed to be sent via the designated ConnectionEndpoint.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if no connection of that name could be found in the connection manager
	 * @throws EndpointIsNotConnectedException 
	 * 		if the specified endpoint is not connected to their partner
	 */
	public void sendMessage(String connectionID, NetworkPackage message) throws EndpointIsNotConnectedException, ManagerHasNoSuchEndpointException {
		if (connections.get(connectionID) == null) {
			ManagerHasNoSuchEndpointException e = new ManagerHasNoSuchEndpointException(connectionID);
			conManLog.logWarning("[CM " + localName + "(" + localPort +")] Failed to push a message of type " + message.getType() + " through CE with ID " + connectionID, e);
			throw e;
		} else {
			conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Pushing a message of type " + message.getType() + " through CE with ID " + connectionID);
			connections.get(connectionID).pushMessage(message);
		}
	}
	
	/**Returns a ConnectionEndpoint if one by the given name was found. Returns NULL and a Warning otherwise.
	 * 
	 * @param connectionName the Identifier of the intended connectionEndpoint. 
	 * @return ConnectionEndpoint 
	 * 		the CE of the given name <br>
	 * 		may be null if no such CE exists.
	 */
	public ConnectionEndpoint getConnectionEndpoint(String connectionName) {
		if(connections.containsKey(connectionName)) {
			return connections.get(connectionName);
		} else {
			return null;
		}
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
	 * @param connectionName	the Identifier of the intended connectionEndpoint.
	 * @return ConnectionState	the State of the ConnectionEndpoint expressed as aConnectionStateEnum.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if no connection of that name could be found in the connection manager
	 */
	public ConnectionState getConnectionState(String connectionName) throws ManagerHasNoSuchEndpointException {
		ConnectionEndpoint ce = getConnectionEndpoint(connectionName);
		if(ce != null) {
			return ce.reportState();
		} else {
			throw new ManagerHasNoSuchEndpointException(connectionName);
		}
	}
	
	/**Changes the current local IP address to a new value. 
	 * This closes all {@linkplain ConnectionEndpoint}s managed by this manager, 
	 * and sets their local IP address accordingly.
	 * @param newLocalAddress the new local IP address
	 */
	public void setLocalAddress(String newLocalAddress) {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Setting local address " + localAddress + " to a new address: " + newLocalAddress);
		closeAllConnections();
		localAddress = newLocalAddress;
		for (ConnectionEndpoint ce : connections.values()) {
			ce.updateLocalAddress(newLocalAddress);
		}

	}
	
	/** Changes the current local port to a new value.
	 * This closes all {@linkplain ConnectionEndpoint}s managed by this manager, 
	 * and sets their local port accordingly.
	 * @param newLocalPort the new local port
	 */
	public void setLocalPort(int newLocalPort) {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Set local port to a new name: " + newLocalPort);
		closeAllConnections();
		localPort = newLocalPort;
		for (ConnectionEndpoint ce : connections.values()) {
			ce.updatePort(newLocalPort);
		}
	}
	
	/**
	 * Updates the local name that ConnectionEndpoints in this manager use when introducing
	 * themselves at connection creation.
	 * @param name
	 * 		the new name
	 */
	public void setLocalName(String name) {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Set local name to a new name: " + name);
		this.localName = name;
	}
	
	/**
	 * @return the local address value currently in use by all {@linkplain ConnectionEndpoint}s.
	 */
	public String getLocalAddress() {
		return localAddress;
	}
	
	/**
	 * @return the local port value currently in use by all {@linkplain ConnectionEndpoint}s.
	 */
	public int getLocalPort() {
		return localPort;
	}

	/**
	 * @return the local name that ConnectionEndpoints in this manager use when introducing
	 * themselves at connection creation.
	 */
	public String getLocalName() {
		return localName;
	}
	
	/**
	 * @return the communication list associated with this ConnectionManager <br>
	 * when accepting incoming connection requests, this list will be checked
	 * if entries exist for that ip:port pair, and if they do, the new CE's
	 * name and public key will be set accordingly
	 */
	public CommunicationList getCommList() {
		return commList;
	}
	
	/**
	 * @param commList
	 * 		the communication list to be associated with this ConnectionManager <br>
	 * 		when accepting incoming connection requests, this list will be checked
	 * 		if entries exist for that ip:port pair, and if they do, the new CE's
	 * 		name and public key will be set accordingly
	 */
	public void setCommList(CommunicationList commList) {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Set CommunicationList to a new list of class " + commList.getClass().getSimpleName());
		this.commList = commList;
	}

	
	/**Closes a named connection if existing and open. Does not destroy the connectionEndpoint, use destroyConnectionEndpoint for that. <br>
	 * @implNote Calls {@linkplain ConnectionEndpoint#closeWithTerminationRequest()}. 
	 * If the request can not be sent, it then calls {@linkplain ConnectionEndpoint#forceCloseConnection()}.
	 * @param connectionName	
	 * 		the name of the intended {@linkplain ConnectionEndpoint}
	 * @return true if the connection was closed, false if no such connection could be found
	 */
	public boolean closeConnection(String connectionName) {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Closing ConnectionEndpoint " + connectionName);
		ConnectionEndpoint ce = getConnectionEndpoint(connectionName);
		if (ce == null) {
			return false;
		} else {
			try {
				ce.closeWithTerminationRequest();
			} catch (EndpointIsNotConnectedException e) {
				ce.forceCloseConnection();
			}
			return true;
		}
	}
	
	/**Closes all connections if existing and open. Does not destroy the connectionEndpoints, use destroyAllConnectionEndpoints for that.
	 * 
	 */
	public void closeAllConnections() {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Closing all ConnectionEndpoints.");
		connections.forEach((k,v) -> closeConnection(k));
	}

	/**
	 * Closes the connection of a specified {@linkplain ConnectionEndpoint} and removes it from the manager.
	 * 
	 * @param connectionID the ID of the {@linkplain ConnectionEndpoint} that is no longer needed.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if there is no {@linkplain ConnectionEndpoint} with the specified name in the manager
	 */
	public void destroyConnectionEndpoint(String connectionID) throws ManagerHasNoSuchEndpointException {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Destroying ConnectionEndpoint " + connectionID);
		ConnectionEndpoint ce = connections.get(connectionID);
		if (ce == null) {
			throw new ManagerHasNoSuchEndpointException(connectionID);
		} else {
			closeConnection(connectionID);
			connections.remove(connectionID);
			
		}
	}
	
	/**
	 * Closes all connections managed by this manager, and removes them from the manager.
	 */
	public void destroyAllConnectionEndpoints() {
		conManLog.logInfo("[CM " + localName + "(" + localPort +")]  Destroying all ConnectionEndpoints.");
		connections.forEach((k,v) -> {
			try {
				connections.get(k).closeWithTerminationRequest();
			} catch (EndpointIsNotConnectedException e) {
				connections.get(k).forceCloseConnection();
			}
		});
		connections.clear();
	}
	
	/**This method is used by the Photon Source to completely remove a connection from a CE that was used to send the Photon Source a Signal.
	 * 
	 * @param ceID the local CE that is either sending the signal and then gets deleted(remoteCall == true) or the local CE that should delete itself after receiving the SOURCE_DESTROY Signal.
	 */
	public void destroySourceConnection(String ceID, boolean remoteCall){
		if(remoteCall) {
			System.out.println("Received CE DestructionRequest from PhotonSource.");
			QuantumnetworkControllcenter.guiWindow.removeCEEntry(ceID);
		}else {
			System.out.println("Starting PhotonSource CE Destruction.");
			try {
				//Send Deletion Request to Remote CE
				NetworkPackage msg = new NetworkPackage(TransmissionTypeEnum.KEYGEN_SOURCE_DESTROY, false); 
				sendMessage(ceID, msg);
				
				//Delete local CE and remove GUI representation
				QuantumnetworkControllcenter.guiWindow.removeCEEntry(ceID);
				
			} catch (EndpointIsNotConnectedException | ManagerHasNoSuchEndpointException e) {
				e.printStackTrace();
			}
		}
		
	}
}
