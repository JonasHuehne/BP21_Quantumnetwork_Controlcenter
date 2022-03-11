package networkConnection;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;

import exceptions.EndpointIsNotConnectedException;
import keyGeneration.KeyGenerator;
import messengerSystem.MessageSystem;


/**Represents a single connection endpoint at a given port, that can connect to a single other connection endpoint on the same machine, in the same local network or via the Internet.
 * Handles connection management and low-level transmissions. Use waitForConnection() and establishConnections() to connect 2 endpoints. Do not call pushMessage()
 *  manually unless you know what you are doing. Use connectionManager.sendMessage() at the very least. Ideally you would use one of the methods from MessageSystem to send anything.
 * This class also contains parsing for different message types in processMessage(). This is used to trigger additional functionality from messages that are f.ex. used to close the connection.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ConnectionEndpoint implements Runnable{
	
	//Local information
	/** Name of this ConnectionEndpoint, used to identify it, e.g. in {@linkplain MessageSystem}. Generally the same as {@link #remoteName}. */
	private String connectionID; // TODO Possibly refactor connectionID and remoteName to be the same
	/** a private instance of KeyGenerator that will be used if this particular ConnectionEndpoint is generating a new Key. */
	private KeyGenerator keyGen;
	
	//Addresses, Sockets and Ports
	/** Our local IP, sent when trying to establish a connection, so that the partner knows which IP to connect to */
	private String localAddress;
	/** When sending a connection request, our CE tells the other CE that we will be receiving messages on this port
	 *  generally this will be the port of the ConnectionManager managing this CE */
	private int localServerPort;
	/** Connected to the ServerSocket of this CE's partner */
	public Socket localClientSocket;
	/** ClientSocket of another CE, set if we accept an incoming connection instead of sending a request */
	private Socket remoteClientSocket;	
	/** IP of the partner CE */
	private String remoteIP;
	/** Port of the partner CE */
	private int remotePort;	
	/** Name of the connected partner, used, for example, in the chat window. Note that this is not the {@link #connectionID} of the partner's CE. */
	private String remoteName;
	/** When establishing a connection with another connection endpoint, this CE sends this name to be set as the "remote name" on the other end */
	private String localName;
	/** public key for verifying messages received on this CE */
	private String publicKey;
	
	//Communication Channels
	/** Outgoing messages to the other CE are sent along this channel */
	private ObjectOutputStream clientOut;
	/** Incoming messages from the other CE are received on tis channel */
	private ObjectInputStream clientIn;	
	
	//State
	/** whether this endpoint is currently listening for incoming messages or not*/
	private boolean isListeningForMessages = false;
	/** true if the endpoint is connected to another endpoint, and has received a {@linkplain TransmissionTypeEnum#CONNECTION_CONFIRMATION} */
	private boolean isConnected = false;
	/** true if the client socket has connected to a server socket, but a connection between endpoints has not been established yet */
	private boolean isBuildingConnection = false;
	
	private Thread messageThread = new Thread(this, connectionID + "_messageThread");	//a parallel thread used to listen for incoming messages while connected to another ConnectionEndpoint.
	
	/** Log of all packages received by this CE */
	private ArrayList<NetworkPackage> packageLog = new ArrayList<NetworkPackage>();
	/** A simple list of text messages received by this CE, for chat log purposes */
	private ArrayList<String> chatLog = new ArrayList<String>();
	
	/** Timeout in ms when trying to connect to a remote server, 0 is an infinite timeout */
	private final int CONNECTION_TIMEOUT = 3000;
	
	
	/**
	 * Used when creating a ConnectionEndpoint as a response to a ConnectionRequest.
	 * Called by {@linkplain ConnectionEndpointServerHandler}. Not intended to be called from anywhere else.
	 * @param connectionName
	 * 		name of the partner that this connection request came from <br>
	 * 		will be the {@link #connectionID} of this endpoint, and the {@link #remoteName}
	 * @param localAddress
	 * 		our local IP address
	 * @param localSocket
	 * 		socket created by ServerSocket.accept(), must be connected, may not be closed
	 * @param targetIP
	 * 		IP of the partner that sent the connection request
	 * @param targetPort
	 * 		server port of the partner that sent the connection request, outgoing messages will be sent to this port
	 * @param localPort
	 * 		our server port, that we receive messages on
	 * @param localName
	 * 		our name that we tell the other ConnectionEndpoint in response to the connection request
	 */
	public ConnectionEndpoint(String connectionName, String localAddress, Socket localSocket, 
							  ObjectOutputStream streamOut, ObjectInputStream streamIn, String targetIP, 
							  int targetPort, int localPort, String localName) {
		this.connectionID = connectionName;
		this.keyGen = new KeyGenerator(this);
		this.localAddress = localAddress;
		this.localServerPort = localPort;
		this.localName = localName;
		System.out.println("Initialized local ServerSocket in Endpoint of " + connectionID + " at Port " + String.valueOf(localServerPort));
		
		localClientSocket = localSocket;
		clientOut = streamOut;
		clientIn = streamIn;
		remoteIP = targetIP;
		remotePort = targetPort;
		System.out.println("+++CE "+ connectionID +" received Socket and Streams!+++");
		
		isBuildingConnection = false;
		isConnected = true;
		
		System.out.println("+++CE "+ connectionID +" is sending a message back!+++");
		try {
			MessageArgs args = new MessageArgs(localName);
			NetworkPackage connectionConfirmation = new NetworkPackage(TransmissionTypeEnum.CONNECTION_CONFIRMATION, args, false);
			pushMessage(connectionConfirmation);
		} catch (EndpointIsNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Wait for greeting
		//System.out.println("[" + connectionID + "]: Waiting for Greeting from connecting Party");
		listenForMessage();
	}
	
	/**
	 * Used when creating a ConnectionEndpoint that tries to connect to another ConnectionEndpoint by sending a request. <br>
	 * Outside of testing, this is only intended to be called by the {@linkplain ConnectionManager} class.
	 * @param connectionID
	 * 		a unique ID for this endpoint, used to identify it in the manager
	 * @param targetIP
	 * 		IP of the partner that sent the connection request
	 * @param targetPort
	 * 		server port of the partner that sent the connection request, outgoing messages will be sent to this port
	 * @param localIP
	 * 		our local IP address (this is the IP we tell the remote endpoint to send messages back to)
	 * @param localPort
	 * 		our server port, that we receive messages on (this is the port we tell the remote endpoint to send messages to)
	 * @param localName
	 * 		when establishing a connection with another CE, this is the name that we give them
	 * @param pk
	 * 		public key that will be used to sign messages sent to this endpoint
	 */
	public ConnectionEndpoint(String connectionID, String targetIP, int targetPort, String localIP, int localPort, String localName, String pk) {
		System.out.println("---A new CE has been created! I am named: "+ connectionID +" and my own IP is: "+ localAddress +" and I am going to connect to :"+ targetIP+":"+targetPort +".--");
		this.connectionID = connectionID;
		this.keyGen = new KeyGenerator(this);

		this.localAddress = localIP;
		this.localServerPort = localPort;
		this.localName = localName;
		
		remoteIP = targetIP;
		remotePort = targetPort;

		try {

			establishConnection(targetIP, targetPort);
		} catch (IOException e) {
			System.err.println("Error occured while establishing a connection from " + connectionID + " to target ServerSocket @ " + remoteIP + ":" + remotePort + ".");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @return the ID of this connection endpoint, used as a unique identifier
	 */
	public String getID() {
		return this.connectionID;
	}
	
	/**Reports the current State of this Endpoints Connection.
	 * 
	 * @return returns the Connection State as a ConnectionStateEnum.
	 */
	public ConnectionState reportState() {
		if(isConnected && !isBuildingConnection) {
			return ConnectionState.CONNECTED;
		}
		if(!isConnected && !isBuildingConnection && !isListeningForMessages) {
			return ConnectionState.CLOSED;
		}
		if(!isConnected && !isBuildingConnection && isListeningForMessages) {
			return ConnectionState.WAITINGFORMESSAGE;
		}
		if(!isConnected && isBuildingConnection) {
			return ConnectionState.CONNECTING;
		}
		return ConnectionState.ERROR;
	}
	
	/**Allows access to the Key Generator that is responsible for this connectionEndpoint.
	 * 
	 * @return	The assigned Key Generator.
	 */
	public KeyGenerator getKeyGen() {
		return keyGen;
	}
	
	
	/**
	 * Updates the local IP address.
	 * If this CE is connected, the connection is closed. 
	 * A connection termination transmission will be sent if possible, 
	 * but if not, the connection will be closed regardless.
	 * @param newLocalAddress
	 */
	public void updateLocalAddress(String newLocalAddress) {
		try {
			closeWithTerminationRequest();
		} catch (EndpointIsNotConnectedException e) {
			forceCloseConnection();
		}
		localAddress = newLocalAddress;
	}
	
	/**returns the local IP
	 * 
	 * @return the local IP
	 */
	public String getLocalAddress() {
		return localAddress;
	}
	
	/**Updates the local Port
	 * If this CE is connected, the connection is closed. 
	 * A connection termination transmission will be sent if possible, 
	 * but if not, the connection will be closed regardless.
	 * @param newPort the new Port
	 */
	public void updatePort(int newPort) {
		try {
			closeWithTerminationRequest();
		} catch (EndpointIsNotConnectedException e) {
			forceCloseConnection();
		}
		localServerPort = newPort;
	}
	
	/**Returns the number of the port that this ConnectionEndpoint uses to listen to incoming messages.
	 * 
	 * @return the Port Number used by this ConnectionEndpoint as an int.
	 */
	public int getServerPort() {
		return localServerPort;
	}
	
	/**Returns the address this ConnectionEndpoint is currently connected to. May be "" or out-dated if called before a connection has been created or after it has been closed again.
	 * 
	 * @return the ip this ConnectionEndpoint is connected to.
	 */
	public String getRemoteAddress() {
		return remoteIP;
	}
	
	/**Works just like getRemoteAddress() but returns the port this ConnectionEndpoint is currently connected to.
	 * Note: this is the port of the OTHER ConnectionEndpoint, that this one is connected to, not this ones own serverPort.
	 * 
	 * @return the port we are sending messages to
	 */
	public int getRemotePort() {
		return remotePort;
	}
	
	/**Returns the Name of the Connected User.
	 * 
	 * @return the Name of the Connected User.
	 */
	public String getRemoteName() {
		return remoteName;
	}
	
	/**Sets the Name of the Connected User.
	 * 
	 * @param remoteName the Name of the connected User.
	 */
	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}
	
	/**
	 * @return 
	 * 		the public key that will be used when verifying messages received on this CE
	 */
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * @param publicKey
	 * 		the public key that will be used when verifying messages received on this CE
	 */
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
	//-------------//
	// Client Side //
	//-------------//
	
	/**Tries to connect to another Endpoints ServerSocket
	 * 
	 * @param targetServerIP	the IP or Name of the other Party.
	 * @param targetServerPort	the Port on which the other Parties ServerSocket is listening for connections.
	 * @throws IOException this is thrown if the connection attempt to the other ConnectionEndpoint fails.
	 */
	public void establishConnection(String targetServerIP, int targetServerPort) throws IOException {
		if(isConnected) {
			System.out.println("Warning: " + connectionID + " is already connected to " + remoteIP + " at Port " + String.valueOf(remotePort) + "! Connection creation aborted!");
			return;
		}
		System.out.println("[" + connectionID + "]: Attempting to connect " + connectionID + " to: " + targetServerIP + " on port " + String.valueOf(targetServerPort) + "!");

		isBuildingConnection = true;
		remoteIP = targetServerIP;
		remotePort = targetServerPort;
		
		//Try to connect to other Server
		try {
			//Connecting own Client Socket to foreign Server Socket
			localClientSocket = new Socket();
			localClientSocket.connect(new InetSocketAddress(remoteIP, remotePort), CONNECTION_TIMEOUT);
			System.out.println(connectionID + " connected to a server socket.");
			clientOut = new ObjectOutputStream(localClientSocket.getOutputStream());
			System.out.println("Output Stream set for " + connectionID);
			clientIn = new ObjectInputStream(localClientSocket.getInputStream());
			System.out.println("Input Stream set for " + connectionID);
			//Send Message to allow foreign Endpoint to connect with us.
			System.out.println("[" + connectionID + "]: " + connectionID + " is sending a greeting.");
			try {
				MessageArgs args = new MessageArgs(localName, localAddress, localServerPort);
				NetworkPackage connectionRequest = new NetworkPackage(TransmissionTypeEnum.CONNECTION_REQUEST, args, false);
				pushMessage(connectionRequest);
			} catch (EndpointIsNotConnectedException e) {
				// This will not happen unless a programming mistake was made
				e.printStackTrace();
				return;
			}
			System.out.println("[" + connectionID + "]: waiting for response");
	
			listenForMessage();
	
		//Error Messages
		} catch (UnknownHostException e) {
			isConnected = false;
			isBuildingConnection = false;
			if(!(localClientSocket==null)) {
				localClientSocket.close();
			}
			System.err.println("[" + connectionID + "]: Connection could not be established! An Error occured while trying to reach the other party via " + remoteIP + ":" + String.valueOf(remotePort));
			e.printStackTrace();
		} catch (IOException e) {
			isConnected = false;
			isBuildingConnection = false;
			if(!(localClientSocket==null)) {
				localClientSocket.close();
			}
			System.err.println("[" + connectionID + "]: Connection could not be established! An Error occured while connecting to the other Client!");
			throw e;
		}
		
	}
	
	
	/**Closes the connection to another ConnectionEndpoint. Does not inform the other endpoint! For that, use {@link #closeWithTerminationRequest()}.
	 * Can also be used to stop a ConnectionEndpoint from waitingForConnection()
	 * @throws IOException	This will be thrown if the closing of any of the Sockets fails. Needs to be handled depending on the context of and by the caller.
	 */
	public void forceCloseConnection(){
		System.out.println("[" + connectionID + "]: Local Shutdown of ConnectionEndpoint " + connectionID);
		isConnected = false;
		isBuildingConnection = false;
		isListeningForMessages = false;
		
		if(localClientSocket != null) {
			try {
				localClientSocket.close();
			} catch (IOException e) {
				System.err.println("[" + connectionID + "]: Shutdown of localClientSocket failed!");
				e.printStackTrace();
			}
			clientOut = null;
			localClientSocket = null;
		}
		
		if(remoteClientSocket != null) {
			try {
				remoteClientSocket.close();
			} catch (IOException e) {
				System.err.println("[" + connectionID + "]: Shutdown of remoteClientSocket failed!");
				e.printStackTrace();
			}
			remoteClientSocket = null;
		}		
	}
	
	/**
	 * Sends a request to the partner CE to close the connection, then closes the connection from this end.
	 * @throws EndpointIsNotConnectedException 
	 * 		if the state of this endpoint is not {@linkplain ConnectionState#CONNECTED} <br>
	 * 		if this is thrown, the state of this endpoint will not have changed
	 */
	public void closeWithTerminationRequest() throws EndpointIsNotConnectedException {
		NetworkPackage terminationRequest = new NetworkPackage(TransmissionTypeEnum.CONNECTION_TERMINATION, new MessageArgs(), false);
		pushMessage(terminationRequest);
		forceCloseConnection();
	}

	/**
	 * Pushes a message through to the endpoint connected to this one.
	 * Expects this endpoint to be {@linkplain ConnectionState#CONNECTED} to its partner, unless type is {@linkplain TransmissionTypeEnum#CONNECTION_REQUEST}.
	 * @param message
	 * 		the message to send
	 * @throws EndpointIsNotConnectedException 
	 * 		if this connection endpoint is not {@linkplain ConnectionState#CONNECTED} to its partner <br>
	 * 		never thrown if type is {@linkplain TransmissionTypeEnum#CONNECTION_REQUEST}
	 */
	public void pushMessage(NetworkPackage message) throws EndpointIsNotConnectedException {
		// NetworkPackages are only send if it's either a connection request, or we are connected
		TransmissionTypeEnum type = message.getType();
		if (   !type.equals(TransmissionTypeEnum.CONNECTION_REQUEST)	
				&& !reportState().equals(ConnectionState.CONNECTED)) {
				throw new EndpointIsNotConnectedException(connectionID, " push message of type " + type);
		}

		try {
			clientOut.writeObject(message);
		} catch (IOException e) {
			// TODO Think about how to handle this
			System.err.println("An I/O Exception occurred trying to push a message to the other endpoint.");
			System.err.println(e.getClass().getSimpleName() + " : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	//-------------//
	// Server Side //
	//-------------//	
		
	/**
	 * Starts a parallel thread causing this endpoint to listen for incoming messages.
	 */
	public final void listenForMessage() {
		if(isListeningForMessages) {
			System.err.println("[" + connectionID + "]: Already listening for Message, not starting a 2. Thread.");
			return;
		}
		isListeningForMessages = true;
		//System.out.println("[" + connectionID + "]: Waiting for Message has started!");
		messageThread.start();
		return;			
	}
	
	/**Processing-step to filter out ConnectionCommands and take appropriate actions depending on the TransmissionType and Argument.
	 * 
	 * @param transmission	the transmission that was just received and should be checked for keywords in the header.
	 */
	private void processMessage(NetworkPackage transmission) {

		System.out.println("[" + connectionID + "]: Received Message, starting processing!: " + transmission.getType() + " - " + transmission.getMessageArgs() + " - " + transmission.getContent());
		// If message sender requested the message to be confirmed, do so
		if (transmission.expectedToBeConfirmed()) {
			NetworkPackage confirmation = 
			new NetworkPackage(TransmissionTypeEnum.RECEPTION_CONFIRMATION, new MessageArgs(), transmission.getID(), false);
			try {
				pushMessage(confirmation);
			} catch (EndpointIsNotConnectedException e) {
				// Log it if no confirmation could be sent, but otherwise continue processing the message as normal
				System.err.println("Could not confirm message with ID " + Base64.getEncoder().encodeToString(transmission.getID()) + ".");
			}
		}
		
		// log the received message
		logPackage(transmission);
		
		//Chose processing based on transmissionType in the NetworkPackage head.
		if (transmission.getType().equals(TransmissionTypeEnum.CONNECTION_CONFIRMATION)) {
			System.out.println("[" + connectionID + "]: Connection Confirmation received!");
			remoteName = transmission.getMessageArgs().userName();
			isBuildingConnection = false;
			isConnected = true;
			return;
		} else {
			try {
				NetworkPackageHandler.handlePackage(this, transmission);
			} catch (EndpointIsNotConnectedException e) {
				// These should both be thrown only during key generation
				System.err.println();
				// For safety, shut down the key generator
				e.printStackTrace();
				// TODO Consider if further error handling should take place, like shutting down the key gen
			}
		}
	}
	
	/**
	 * While the Client is connecting or connected, this will listen for incoming messages if {@link #isListeningForMessages} 
	 * is true and pass them to {@link #processMessage(NetworkPackage)}. This is usually started by listenForMessage().
	 * <b> Calling this directly from the outside is discouraged. </b>
	 */
	@Override
	public void run() {
		isListeningForMessages = true;
		NetworkPackage receivedMessage;
		while(isListeningForMessages) {
			try {
				if(isListeningForMessages && clientIn != null && (isConnected||isBuildingConnection) && (receivedMessage = (NetworkPackage) clientIn.readObject()) != null) {
					System.out.println( "[" + connectionID + "]:Starting to process Message...");
					processMessage(receivedMessage);

				}
			} catch (IOException | ClassNotFoundException e) {
				if(isConnected) {
					System.err.println("[" + connectionID + "]: Error while waiting for Message at " + connectionID + "!");
					e.printStackTrace();
				}
			}
		}
		isListeningForMessages = false;
	}

	/**
	 * Adds a message / package to the log.
	 * If it is a NetworkPackage, its contents will be cleared.
	 * @param msg
	 * 		the message to add
	 */
	public void logPackage(NetworkPackage msg) {
		if(msg.getType().equals(TransmissionTypeEnum.FILE_TRANSFER)) msg.clearContents();
		packageLog.add(msg);
	}
	
	/**@return
	 * Network packages received by this ConnectionEndpoint.
	 * For packages of type FILE_TRANSFER the content will be empty.
	 */
	public ArrayList<NetworkPackage> getPackageLog() {
		return packageLog;
	}
	
	/**
	 * Filters the package log for messages of a certain type.
	 * @param type
	 * 		the type of package to filter for
	 * @return
	 * 		package of the given type from the log
	 */
	public ArrayList<NetworkPackage> getLoggedPackagesOfType(TransmissionTypeEnum type) {
		ArrayList<NetworkPackage> filtered = new ArrayList<>();
		for (NetworkPackage np : packageLog) {
			if (np.getType().equals(type)) filtered.add(np);
		}
		return filtered;
	}

	/**
	 * @return the current chat log
	 */
	public ArrayList<String> getChatLog() {
		return chatLog;
	}

	/**
	 * Adds a message to the chat log.
	 * @param sent
	 * 		true if the message was sent from this CE <br>
	 * 		false if it was received
	 * @param message
	 * 		the message to add
	 */
	public void appendMessageToChatLog(boolean sent, String message) {
		String sender = sent ? "You" : remoteName;
		this.chatLog.add(sender + " wrote: " + message);
	}

}
