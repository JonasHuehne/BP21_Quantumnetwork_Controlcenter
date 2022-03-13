package networkConnection;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import exceptions.CouldNotDecryptMessageException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.VerificationFailedException;
import keyGeneration.KeyGenerator;
import messengerSystem.MessageSystem;
import qnccLogger.Log;


/**Represents a single connection endpoint at a given port, that can connect to a single other connection endpoint on the same machine, in the same local network or via the Internet.
 * Handles connection management and low-level transmissions. Use waitForConnection() and establishConnections() to connect 2 endpoints. Do not call pushMessage()
 *  manually unless you know what you are doing. Use connectionManager.sendMessage() at the very least. Ideally you would use one of the methods from MessageSystem to send anything.
 * This class also contains parsing for different message types in processMessage(). This is used to trigger additional functionality from messages that are f.ex. used to close the connection.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ConnectionEndpoint implements Runnable{
	
	private Log ceLogger;
	
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
	/** ID of the key entry in the key store that this CE will use for encryption and decryption */
	private String keyStoreID;
	
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
	/** A simple chat log for this CE. Each Entry is of the form (Sender, Message). */
	private ArrayList<SimpleEntry<String, String>> chatLog = new ArrayList<SimpleEntry<String, String>>();
	
	/** MessageIDs of messages for which this endpoint received a confirmation after sending them */
	private ArrayList<byte[]> receivedConfirmations = new ArrayList<byte[]>();
	/** This hashmap acts like a queue. Add a pair {@code (i, msg)} here, and once a RECEPTION_CONFIRMATION is received 
	 * which confirms the message with the ID {@code i}, the entry with key {@code i} is pushed via {@linkplain ConnectionEndpoint#pushMessage(NetworkPackage)}. 
	 * Useful when waiting for key use to be approved. Potentially (?) for splitting up larger messages as well.  */
	private HashMap<String, NetworkPackage> pushOnceApproved = new HashMap<String, NetworkPackage>(); 
	// uses String instead of byte[] because equals() for two byte arrays in a HashMap only returns true if they are same object, not same contents
	
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
		this.ceLogger = new Log("CE Logger [ID " + connectionName + "]");
		
		ceLogger.logInfo("[CE " + connectionName + "] Creation of CE in response to an incoming connection request has begun. "
						+ "CE will be connected to " + targetIP + ":" + targetPort + " and is in the CM with port " + localPort);
		this.connectionID = connectionName;
		this.keyGen = new KeyGenerator(this);
		this.localAddress = localAddress;
		this.localServerPort = localPort;
		this.localName = localName;
		this.localClientSocket = localSocket;
		this.clientOut = streamOut;
		this.clientIn = streamIn;
		this.remoteIP = targetIP;
		this.remotePort = targetPort;
		this.isBuildingConnection = false;
		this.isConnected = true;
		
		ceLogger.logInfo("[CE " + connectionName + "] Local values have been set. Now sending a connection confirmation to the partner CE. ");
		
		try {
			MessageArgs args = new MessageArgs(localName);
			NetworkPackage connectionConfirmation = new NetworkPackage(TransmissionTypeEnum.CONNECTION_CONFIRMATION, args, false);
			pushMessage(connectionConfirmation);
		} catch (EndpointIsNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ceLogger.logInfo("[CE " + connectionName + "] Connection confirmation sent. Now listening for messages. ");
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
		this.ceLogger = new Log("CE Logger [ID " + connectionID + "]");
		
		ceLogger.logInfo("[CE " + connectionID + "] Creation of CE in response to a local request has begun. "
				+ "CE will attempt to connect to " + targetIP + ":" + targetPort + " and is in the CM with port " + localPort);
		
		this.connectionID = connectionID;
		this.keyGen = new KeyGenerator(this);
		this.localAddress = localIP;
		this.localServerPort = localPort;
		this.localName = localName;
		this.publicKey = pk;
		this.remoteIP = targetIP;
		this.remotePort = targetPort;
		ceLogger.logInfo("[CE " + connectionID + "] Local values have been set. Now attempting to establish a connection. ");
		try {
			establishConnection(targetIP, targetPort);
		} catch (IOException e) {
			System.err.println("Error occurred while establishing a connection from " + connectionID + " to target ServerSocket @ " + remoteIP + ":" + remotePort + ".");
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
			ceLogger.logWarning("Warning: " + connectionID + " is already connected to " + remoteIP + " at Port " + String.valueOf(remotePort) + "! Connection creation aborted!");
			return;
		}
		ceLogger.logInfo("[CE " + connectionID + "] Attempting to connect " + connectionID + " to: " + targetServerIP + " on port " + String.valueOf(targetServerPort) + "!");

		isBuildingConnection = true;
		remoteIP = targetServerIP;
		remotePort = targetServerPort;
		
		//Try to connect to other Server
		try {
			//Connecting own Client Socket to foreign Server Socket
			localClientSocket = new Socket();
			localClientSocket.connect(new InetSocketAddress(remoteIP, remotePort), CONNECTION_TIMEOUT);
			ceLogger.logInfo("[CE " + connectionID + "] Local Socket connected to a server socket.");
			clientOut = new ObjectOutputStream(localClientSocket.getOutputStream());
			ceLogger.logInfo("[CE " + connectionID + "] Output Stream set.");
			clientIn = new ObjectInputStream(localClientSocket.getInputStream());
			ceLogger.logInfo("[CE " + connectionID + "] Input Stream set.");
			//Send Message to allow foreign Endpoint to connect with us.
			ceLogger.logInfo("[CE " + connectionID + "] Now sending a connection request on the newly established connection.");
			try {
				MessageArgs args = new MessageArgs(localName, localAddress, localServerPort);
				NetworkPackage connectionRequest = new NetworkPackage(TransmissionTypeEnum.CONNECTION_REQUEST, args, false);
				pushMessage(connectionRequest);
			} catch (EndpointIsNotConnectedException e) {
				// This will not happen unless a programming mistake was made
				e.printStackTrace();
				return;
			}
			ceLogger.logInfo("[CE " + connectionID + "] Now waiting for a response to the connection request.");
	
			listenForMessage();
	
		//Error Messages
		} catch (UnknownHostException e) {
			isConnected = false;
			isBuildingConnection = false;
			if(!(localClientSocket==null)) {
				localClientSocket.close();
			}
			ceLogger.logError("[CE " + connectionID + "]: Connection could not be established! An Error occurred while trying to reach the other party via " + remoteIP + ":" + String.valueOf(remotePort), e);
		} catch (IOException e) {
			isConnected = false;
			isBuildingConnection = false;
			if(!(localClientSocket==null)) {
				localClientSocket.close();
			}
			ceLogger.logError("[CE " + connectionID + "]: Connection could not be established! An Error occured while connecting to the other Client!", e);
			throw e;
		}
		
	}
	
	
	/**Closes the connection to another ConnectionEndpoint. Does not inform the other endpoint! For that, use {@link #closeWithTerminationRequest()}.
	 * Can also be used to stop a ConnectionEndpoint from waitingForConnection()
	 * @throws IOException	This will be thrown if the closing of any of the Sockets fails. Needs to be handled depending on the context of and by the caller.
	 */
	public void forceCloseConnection(){
		ceLogger.logInfo("[CE " + connectionID + "]: Local Shutdown of ConnectionEndpoint " + connectionID);
		isConnected = false;
		isBuildingConnection = false;
		isListeningForMessages = false;
		
		if(localClientSocket != null) {
			try {
				localClientSocket.close();
			} catch (IOException e) {
				ceLogger.logError("[CE " + connectionID + "]: Shutdown of localClientSocket failed!", e);
			}
			clientOut = null;
			localClientSocket = null;
		}
		
		if(remoteClientSocket != null) {
			try {
				remoteClientSocket.close();
			} catch (IOException e) {
				ceLogger.logError("[" + connectionID + "]: Shutdown of remoteClientSocket failed!", e);
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
		ceLogger.logInfo(("[CE " + connectionID + "]: Pushing message with type " + message.getType() + " and ID " + Base64.getEncoder().encodeToString(message.getID())));
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
			ceLogger.logError("An I/O Exception occurred trying to push a message to the other endpoint.", e);
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
			ceLogger.logWarning("[CE " + connectionID + "]: Already listening for Message, not starting a 2. Thread.");
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

		ceLogger.logInfo("[CE " + connectionID + "] Started processing of message with Type = " + transmission.getType() 
						+ " Arguments = " + transmission.getMessageArgs() + 
						" ID =  "  +  Base64.getEncoder().encodeToString(transmission.getID()) + 
						" Confirm Request = " + transmission.expectedToBeConfirmed());
		
		// If message sender requested the message to be confirmed, do so
		if (transmission.expectedToBeConfirmed()) {
			NetworkPackage confirmation = 
			new NetworkPackage(TransmissionTypeEnum.RECEPTION_CONFIRMATION, new MessageArgs(), transmission.getID(), false);
			try {
				pushMessage(confirmation);
				ceLogger.logInfo(("[CE " + connectionID + "] Sent confirmation for message with ID "  + Base64.getEncoder().encodeToString(transmission.getID())));
			} catch (EndpointIsNotConnectedException e) {
				// Log it if no confirmation could be sent, but otherwise continue processing the message as normal
				ceLogger.logError("[CE " + connectionID + "] Could not confirm message with ID " + Base64.getEncoder().encodeToString(transmission.getID()) + ".", e);
			}
		}
		
		//Chose processing based on transmissionType in the NetworkPackage head.
		if (transmission.getType().equals(TransmissionTypeEnum.CONNECTION_CONFIRMATION)) {
			remoteName = transmission.getMessageArgs().userName();
			isBuildingConnection = false;
			isConnected = true;
			ceLogger.logInfo("[CE " + connectionID + "]: Connection Confirmation received! RemoteName = " + remoteName);
			return;
		} else {
			try {
				NetworkPackageHandler.handlePackage(this, transmission);
			} catch (EndpointIsNotConnectedException e) {
				// This should currently only be thrown during key generation
				// For safety, shut down the key generator
				try { this.keyGen.shutdownKeyGen(false, true); } 
				catch (EndpointIsNotConnectedException e1) { e1.printStackTrace(); }
			} catch (CouldNotDecryptMessageException e) {
				// Only thrown if a message is received that could not be decrypted
				// TODO log this event
			} catch (VerificationFailedException e) {
				// Only thrown if a message is received where the signature was not valid
				// OR where no pk could be found (currently: checking commlist for entries with name == ID of this CE)
				// TODO log this event
			} 
			// in any case, log the received message
			logPackage(transmission);
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
					ceLogger.logInfo( "[CE " + connectionID + "] Received a message of type " + receivedMessage.getType() + " beginning processing now.");
					processMessage(receivedMessage);

				}
			} catch (IOException | ClassNotFoundException e) {
				if(isConnected) {
					ceLogger.logError("[CE " + connectionID + "]: Error while waiting for Message at " + connectionID + "!", e);
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
	 * @return the current chat log <br>
	 * the key of each entry is the sender, the value the message
	 */
	public ArrayList<SimpleEntry<String, String>> getChatLog() {
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
		String sender = sent ? localName + " (You)" : remoteName;
		this.chatLog.add(new SimpleEntry<>(sender, message));
	}

	/**
	 * @return the keyStoreID <br>
	 * this is the ID of the key in the keystore, which this CE will use for message encryption and decryption
	 */
	public String getKeyStoreID() {
		return keyStoreID;
	}

	/**
	 * @param keyStoreID the keyStoreID to set <br>
	 * this is the ID of the key in the keystore, which this CE will use for message encryption and decryption
	 */
	public void setKeyStoreID(String keyStoreID) {
		this.keyStoreID = keyStoreID;
	}

	/**
	 * @return a list of message IDs - these are the IDs of the messages sent by this CE,
	 * for which it received a corresponding message of type {@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION}
	 */
	public ArrayList<byte[]> getConfirmations() {
		return receivedConfirmations;
	}
	
	/**
	 * Checks whether this CE has received a confirmation for a message with the given ID.
	 * @param messageID
	 * 		the ID to check
	 * @return
	 * 		true if it has, false if not
	 */
	public boolean hasConfirmationFor(byte[] messageID) {
		for (byte[] id : receivedConfirmations) {
			if (Arrays.equals(id, messageID)) return true;
		}
		return false;
	}
	
	/**
	 * Adds the given message ID to the list of message IDs for which this endpoint received a confirmation. <br>
	 * @implNote Currently it is not checked whether this endpoint actually sent a message with the given ID,
	 * it is expected that this is done before adding it to the list.
	 * @param messageID
	 * 		ID to add
	 */
	public void addConfirmationFor(byte[] messageID) {
		receivedConfirmations.add(messageID);
	}
	
	/**
	 * This will push the given NetworkPackage once this CE receives a confirmation, 
	 * which confirms the message the message with the given ID.
	 * (Meaning a {@linkplain TransmissionTypeEnum#RECEPTION_CONFIRMATION} where the content field is == id.)
	 * @param id
	 * 		the ID to wait for confirmation for
	 * @param message
	 * 		the message to send once confirmation for that ID arrives
	 */
	public void pushOnceConfirmationReceivedForID(byte[] id, NetworkPackage message) {
		String stringID = Base64.getEncoder().encodeToString(id);
		pushOnceApproved.put(stringID, message);
	}
	
	/**
	 * Removes an entry from the push queue that was previously added by {@linkplain #pushOnceConfirmationReceivedForID(byte[], NetworkPackage)}.
	 * @param id
	 * 		the ID to no longer wait for confirmation for
	 * @return
	 * 		the entry removed, or null if there was no such entry
	 */
	public NetworkPackage removeFromPushQueue(byte[] id) {
		String stringID = Base64.getEncoder().encodeToString(id);
		return pushOnceApproved.remove(stringID);
	}
}
