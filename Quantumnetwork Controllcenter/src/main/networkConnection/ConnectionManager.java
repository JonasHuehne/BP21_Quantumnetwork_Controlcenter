package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import exceptions.ConnectionAlreadyExistsException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.PortIsInUseException;

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
	/** This ServerSocket allows other ConnectionEndpoints to connect to us by sending requests to {@link #localAddress}:{@link #localPort}*/
	private ServerSocket masterServerSocket;
	/** This thread continuously checks for incoming connection requests */
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	/** Whether this CM is currently accepting ConnectionRequests */
	private boolean isAcceptingConnections = false;
	/** Used for control flow only */
	private boolean submittedTaskOnce = false;
	
	/** true <==> no two connections to the same IP:Port pairing are allowed (should also make self-connections impossible) <br>
	 *  for actual use we recommend setting this to true, however, it may make some manual tests impossible that involve connecting to oneself */
	private boolean oneConnectionPerIpPortPair = false;


	/**
	 * Creates and new ConnectionManager.
	 * Automatically begins accepting connection requests.
	 * 
	 * @param localAddress 
	 * 		the ip address that is being passed on to any local ConnectionEndpoints, should be our local IP address
	 * @param localPort
	 * 		the port that this ConnectionManager will be accepting connection requests on, and that contained ConnectionEndpoints will be receiving messages on <br>
	 * 		must not be in use by any other ConnectionManager
	 * @throws IOException 
	 * 		if an I/O Exception occurred while trying to open the ServerSocket used for accepting connections
	 * @throws PortIsInUseException
	 * 		if the specified port is already in use by another ConnectionManager <br>
	 * 		ports used by a ConnectionManager remain marked as used until the program is restarted
	 */
	public ConnectionManager(String localAddress, int localPort) throws IOException, PortIsInUseException{
		if (portsInUse.contains(localPort)) throw new PortIsInUseException("Port " + localPort + " is already in use by a ConnectionManager.");
		
		this.localAddress = localAddress;
		this.localPort = localPort;
		
		masterServerSocket = new ServerSocket(this.localPort);
		portsInUse.add(localPort);
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
						clientSocket = masterServerSocket.accept();
						System.out.println("Accepted a request on master server socket.");
					} catch (IOException e) {
						System.err.println("An I/O Exception occurred while trying to accept a connection in the ConnectionManager "
								+ "with local IP " + localAddress + " and server port " + localPort + ". Accepting connections is being shut down.");
						System.err.println("Message: " + e.getMessage());
						isAcceptingConnections = false;
						break;
					} // Check that it is from a ConnectionEndpoint of our program
					
					ConnectionEndpointServerHandler cesh; 
					try { // Construct a CESH for the socket that just connected to our server socket
						cesh = new ConnectionEndpointServerHandler(clientSocket);
						System.out.println("Created CESH for newly received client socket.");
						cesh.run();
					} catch (IOException e) {
						System.err.println("An I/O Exception occurred while trying to construct the ConnectionEndpointServerHandler "
								+ "with local IP " + localAddress + " and server port " + localPort + ". Accepting connections is being shut down.");
						System.err.println("Message: " + e.getMessage());
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
	 * This version of the method should be used if the CE is being created because of the local users intentions and not as part of the response to a connection request
	 * from an external source.
	 * 
	 * The CE will attempt to connect to the targetIP and Port as soon as it is created.
	 * 
	 *	@param endpointName 	
	 *		the identifier for a connection. This name can be used to access it later
	 *	@param targetIP 
	 *		IP of the {@linkplain ConnectionEndpoint} that the newly created CE should connect to
	 *	@param targetPort 	
	 *		server port of the {@linkplain ConnectionEndpoint} that the newly created CE should connect to
	 *	@return ConnectionEndpoint
	 *		the newly created {@linkplain ConnectionEndpoint} <br>
	 *		can also be accessed via {@linkplain #getConnectionEndpoint(String)} with argument {@code endpointName}
	 * 	@throws ConnectionAlreadyExistsException 
	 * 		if a connection with the specified name is already managed by this ConnectionManager
	 * @throws IpAndPortAlreadyInUseException 
	 * 		if a connection with the same IP and Port pairing is already in this ConnectionManager
	 */
	public ConnectionEndpoint createNewConnectionEndpoint(String endpointName, String targetIP, int targetPort, String sig)
		throws ConnectionAlreadyExistsException, IpAndPortAlreadyInUseException {
		if(!connections.containsKey(endpointName)) {
			// no two connections to the same IP / Port pairing
			if (oneConnectionPerIpPortPair && !ipAndPortAreFree(targetIP, targetPort)) throw new IpAndPortAlreadyInUseException(targetIP, targetPort);
			System.out.println("---Received new request for a CE. Creating it now. It will connect to the Server at "+ targetIP +":"+ targetPort +".---");
			connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, localPort, sig, targetIP, targetPort));
			return connections.get(endpointName);
		} else {
			throw new ConnectionAlreadyExistsException(endpointName);

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
				if (oneConnectionPerIpPortPair && !ipAndPortAreFree(targetIP, targetPort)) throw new IpAndPortAlreadyInUseException(targetIP, targetPort);
				System.out.println("---Received new request for a CE. Creating it now. It will connect to the Server at "+ targetIP +":"+ targetPort +".---");
				//connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, localPort, targetIP, targetPort));
				connections.put(endpointName, new ConnectionEndpoint(endpointName, localAddress, localPort, clientSocket, streamOut, streamIn, targetIP, targetPort));
				return connections.get(endpointName);
			} else {
				throw new ConnectionAlreadyExistsException(endpointName);
			}
		}

	/**
	 * Utility method. Used to check if an IP/Port pairing is not used by any connection in the manager at the moment.
	 */
	private boolean ipAndPortAreFree(String ip, int port) {
		for (ConnectionEndpoint ce : connections.values()) {
			if (ce.getRemoteAddress().equals(ip) && ce.getRemotePort() == port) return true;
		}
		return false;
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
	 * @param connectionID	the Identifier of the intended connectionEndpoint.
	 * @param type the type of the transmission expressed as a TransmissionTypeEnum
	 * @param typeArgument an additional String that some transmission types use. Can be "" if not needed.
	 * @param message the String Message that is supposed to be sent via the designated ConnectionEndpoint.
	 * @param sig the optional signature, used by authenticated messages
	 * @param confID the optional ID used to confirm that the message has been received.
	 * @throws ManagerHasNoSuchEndpointException 
	 * 		if no connection of that name could be found in the connection manager
	 * @throws EndpointIsNotConnectedException
	 * 		if the specified connection endpoint is not connected to its partner <br>
	 * 		will not be thrown for transmissions of type {@linkplain TransmissionTypeEnum#CONNECTION_REQUEST}
	 */
	public void sendMessage(String connectionID, TransmissionTypeEnum type, String typeArgument, byte[] message, byte[] sig) throws ManagerHasNoSuchEndpointException, EndpointIsNotConnectedException {
		ConnectionEndpoint ce = connections.get(connectionID);
		if (ce == null) {
			throw new ManagerHasNoSuchEndpointException(connectionID);
		} else {
			connections.get(connectionID).pushMessage(type, typeArgument, message, sig);
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
		closeAllConnections();
		localAddress = newLocalAddress;
		for (ConnectionEndpoint ce : connections.values()) {
			ce.updateLocalAddress(newLocalAddress);
		}
	}

	/**Changes the current local port address to a new value. 
	 * This closes all {@linkplain ConnectionEndpoint}s managed by this manager, 
	 * and sets their local port accordingly.
	 * @param newLocalPort the new local port
	 */
	public void setLocalPort(int newLocalPort) {
		closeAllConnections();
		localPort = newLocalPort;
		for (ConnectionEndpoint ce : connections.values()) {
			ce.updatePort(newLocalPort);
		}
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

	/**Closes a named connection if existing and open. Does not destroy the connectionEndpoint, use destroyConnectionEndpoint for that. <br>
	 * @implNote Calls {@linkplain ConnectionEndpoint#closeWithTerminationRequest()}. 
	 * If the request can not be sent, it then calls {@linkplain ConnectionEndpoint#forceCloseConnection()}.
	 * @param connectionName	
	 * 		the name of the intended {@linkplain ConnectionEndpoint}
	 * @return true if the connection was closed, false if no such connection could be found
	 */
	public boolean closeConnection(String connectionName) {
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
		System.out.println("[ConnectionManager]: Destroying ConnectionEndpoint " + connectionID);
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
		connections.forEach((k,v) -> {
			try {
				connections.get(k).closeWithTerminationRequest();
			} catch (EndpointIsNotConnectedException e) {
				connections.get(k).forceCloseConnection();
			}
		});
		connections.clear();
	}
}
