package networkConnection;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.GenericWarningMessage;
import graphicalUserInterface.MessageGUI;
import keyGeneration.KeyGenerator;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;


/**Represents a single connection endpoint at a given port, that can connect to a single other connection endpoint on the same machine, in the same local network or via the internet.
 * Handles connection management and low-level transmissions. Use waitForConnection() and establishConnections() to connect 2 endpoints. Do not call pushMessage()
 *  manually unless you know what you are doing. Use connectionManager.sendMessage() at the very least. Ideally you would use one of the methods from MessageSystem to send anything.
 * This class also contains parsing for different message types in processMessage(). This is used to trigger additional functionality from messages that are f.ex. used to close the connection.
 * 
 * @author Jonas Huehne
 *
 */
public class ConnectionEndpoint implements Runnable{
	
	//Local information
	private String connectionID;	//the name of the ConnectionEndpoint. Used to identify it when using methods from MessageSystem or when interacting with the ConnectionListDB. Should be named after the target of the connection.
	private KeyGenerator keyGen;	//a private instance of KeyGenerator that will be used if this particular ConnectionEndpoint is generating a new Key.
	
	//Addresses, Sockets and Ports
	private String localAddress;	//the own IP
	private int localServerPort;	//the Port of the local Server that other Endpoints can send Messages to.
	public Socket localClientSocket;	// connects to another Endpoints Serversocket.
	private Socket remoteClientSocket;	// another Endpoints ClientSocket that messaged out local Server Socket	
	private String remoteIP;	//the IP of the connected Endpoint
	private int remotePort;		//the Port of the connected Endpoint
	private String remoteName;	//the Name of the connected Partner
	
	//Communication Channels
	private ObjectOutputStream clientOut;	//the outgoing channel that information from this ConnectionEndpoint is sent to another.
	private ObjectInputStream clientIn;	//the incoming message channel.
	
	//State
	private boolean listenForMessages = false;
	private boolean isConnected = false;
	private boolean isBuildingConnection = false;
	private boolean waitingForMessage = false;
	
	private MessageGUI logGUI;//This is a ref to the Chat GUI
	
	private Thread messageThread = new Thread(this, connectionID + "_messageThread");	//a parallel thread used to listen for incoming messages while connected to another ConnectionEndpoint.
	
	
	private LinkedList<String> pendingConfirmations = new LinkedList<String>();	//this is where reception confirmations are stored and checked from while a confirmedMessage is waiting for a confirmation.
	
	private String messageLog = " ------ START OF MESSAGE LOG ------ ";
	
	/**A ConnectionEndpoint is created with a unique name, an address, that determines the information exchanged during the creation of a connection and
	 * a port that this ConnectionEndpoint is listening on for messages.
	 * 
	 * @param connectionName the name of the ConnectionEndpoint, also used in the ContactListDB and the MessageSystem methods.
	 * @param localAddress	the ip that the ConnectionEndpoint is working on. Is sent to the other ConnectionEndpoint when creating a connection.
	 * @param serverPort	the port this ConnectionEndpoints server is listening on for messages.
	 */
	
	//Use for conResp
	public ConnectionEndpoint(String connectionName, String localAddress, Socket localSocket, ObjectOutputStream streamOut, ObjectInputStream streamIn, String targetIP, int targetPort) {
		connectionID = connectionName;
		keyGen = new KeyGenerator(connectionID);
		this.localAddress = localAddress;
		localServerPort = QuantumnetworkControllcenter.conMan.getConnectionSwitchbox().getMasterServerPort();
		System.out.println("Initialised local ServerSocket in Endpoint of " + connectionID + " at Port " + String.valueOf(localServerPort));
		
		
		localClientSocket = localSocket;
		clientOut = streamOut;
		clientIn = streamIn;
		remoteIP = targetIP;
		remotePort = targetPort;
		System.out.println("+++CE "+ connectionID +" received Socket and Streams!+++");
		
		isBuildingConnection = false;
		isConnected = true;
		
		System.out.println("+++CE "+ connectionID +" is sending a message back!+++");
		pushMessage(TransmissionTypeEnum.CONNECTION_CONFIRMATION, Configuration.getProperty("UserName"), null, null);
		QuantumnetworkControllcenter.guiWindow.createConnectionRepresentation(connectionName, remoteIP, remotePort);
		//Wait for greeting
		//System.out.println("[" + connectionID + "]: Waiting for Greeting from connecting Party");
		listenForMessage();
	}
	
	//Used for conEst
	public ConnectionEndpoint(String connectionName, String targetIP, int targetPort) {
		System.out.println("---A new CE has been created! I am named: "+ connectionName +" and my own IP is: "+ localAddress +" and I am going to connect to :"+ targetIP+":"+targetPort +".--");
		connectionID = connectionName;
		keyGen = new KeyGenerator(connectionID);

		localAddress = QuantumnetworkControllcenter.conMan.getLocalAddress();
		localServerPort = QuantumnetworkControllcenter.conMan.getLocalPort();
		
		remoteIP = targetIP;
		remotePort = targetPort;

		try {

			establishConnection(targetIP, targetPort);
		} catch (IOException e) {
			System.err.println("Error occured while establishing a connection from " + connectionID + " to target ServerSocket @ " + remoteIP + ":" + remotePort + ".");
			e.printStackTrace();
		}
		
	}
	
	/**Reports the current State of this Endpoints Connection.
	 * 
	 * @return returns the Connection State as a ConnectionStateEnum.
	 */
	public ConnectionState reportState() {
		if(isConnected && !isBuildingConnection) {
			return ConnectionState.CONNECTED;
		}
		if(!isConnected && !isBuildingConnection && !waitingForMessage) {
			return ConnectionState.CLOSED;
		}
		if(!isConnected && !isBuildingConnection && waitingForMessage) {
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
	
	
	/**updated the local IP. This is usually called by the ConnectionManager.setLocalAddress()
	 * 
	 * @param newLocalAddress
	 */
	public void updateLocalAddress(String newLocalAddress) {
		closeConnection(true);
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
	 * 
	 * @param newPort the new Port
	 */
	public void updatePort(int newPort) {
		closeConnection(true);
		localServerPort = newPort;
	}
	
	/**Returns the int-Portnumber that this ConnectionEndpoint uses to listen to incoming messages.
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
	 * Note: this is the port of the OTHER ConnectionEndpoint, that this one is connected to, not this owns own serverPort.
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
	
	/**Returns the latest Text-Message Log.
	 * 
	 * @return the latest Text-Message Log.
	 */
	public MessageGUI getLogGUI() {
		return logGUI;
	}
	
	/**Use this to Edit the Text-Message Log.
	 * 
	 * @param log The new Log.
	 */
	public void setLogGUI(MessageGUI log) {
		logGUI = log;
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
			localClientSocket = new Socket(remoteIP,remotePort);
			clientOut = new ObjectOutputStream(localClientSocket.getOutputStream());
			clientIn = new ObjectInputStream(localClientSocket.getInputStream());
			//Send Message to allow foreign Endpoint to connect with us.
			System.out.println("[" + connectionID + "]: " + connectionID + " is sending a greeting.");
			pushMessage(TransmissionTypeEnum.CONNECTION_REQUEST, localAddress + ":::" + localServerPort + ":::" + Configuration.getProperty("UserName"), null, null);
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
	
	
	/**Closes the connection to another ConnectionEndpoint. Can also be used to stop a ConnectionEndpoint from waitingForConnection()
	 * 
	 * @param localRequest true if the request to close the connection has a local origin, false if the request came as a message from the other Endpoint.
	 * @throws IOException	This will be thrown if the closing of any of the 3 Sockets fails. Needs to be handled depending on the context of and by the caller.
	 */
	public void closeConnection(boolean localRequest) {
		if(isConnected && localRequest) {
			//If close-request has local origin, message other connectionEndpoint about closing the connection.
			pushMessage(TransmissionTypeEnum.CONNECTION_TERMINATION, "", null, null);
		}
		System.out.println("[" + connectionID + "]: Local Shutdown of ConnectionEndpoint " + connectionID);
		isConnected = false;
		isBuildingConnection = false;
		waitingForMessage = false;
		listenForMessages = false;
		
	
		
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
	
	/**Pushes a Message to the connected ConnectionEndpoints ServerSocket via the local clientOut Object.
	 * 
	 * @param type the type of message being sent. Regular transmissions should use TransmissionTypeEnum.TRANSMSSION.
	 * @param typeArgument an additional argument used by some TransmissionTypes to pass on important information. Can be "" if not needed.
	 * @param message the String Message that should be send to the connected ConnectionEndpoints Server.
	 * @param sig the signature of the Message if it is an authenticated message. If not, set sig to null.
	 */
	public void pushMessage(TransmissionTypeEnum type, String typeArgument, byte[] message, byte[] sig) {
		//Check for existence of connection before attempting so send.
		if(!isConnected && !isBuildingConnection) {
			System.err.println("[" + connectionID + "]: Warning: Attempted to push a message to another Endpoint while not beeing connected to anything!");
			return;
		}
		//Write Message to Stream
		try {
			clientOut.writeObject(new NetworkPackage(type, typeArgument, message, sig));
		} catch (IOException e) {
			System.err.println("[" + connectionID + "]: Failed when sending off a message via clientOut.");
			e.printStackTrace();
		}
	}

	
	/**This is called in the message processing step after receiving a transmission if the type was a response to a confirmedMessages.
	 * This stores the messageID in the pendingConfirmations List where it will be found by the confirmedMessage method waiting for the confirmation this id is representing.
	 * @param messageID the id that was originally sent as part of the confirmedMessage() Method and was returned back here as the reception confirmation.
	 */
	public void registerConfirmation(String messageID) {
		pendingConfirmations.add(messageID);
	}
	
	/**This returns a clone of the confirmationList. Used to check if a reception confirmation has arrived for the confiremMessage of a given ID.
	 * 
	 * @return a copy of all received confirmations.
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<String> getConfirmations(){
		return (LinkedList<String>) pendingConfirmations.clone();
	}
	
	/**This is called by the waiting confirmMessage Method after the ID it has been waiting for was entered into the pendingConfirmations.
	 * 
	 * @param messageID the id to remove from the pendingConfirmations.
	 */
	public void clearConfirmation(String messageID){
		pendingConfirmations.remove(messageID);
	}
	
	
	
	
	//-------------//
	// Server Side //
	//-------------//	
		
	/**Waits until a message was received and then returns the message. Blocking.
	 * 
	 * @return	returns the String of the next received message.
	 */
	public void listenForMessage() {
		if(listenForMessages) {
			System.err.println("[" + connectionID + "]: Already listening for Message, not starting a 2. Thread.");
			return;
		}
		listenForMessages = true;
		//System.out.println("[" + connectionID + "]: Waiting for Message has started!");
		messageThread.start();
		return;			
	}
	
	/**Processing-step to filter out ConnectionCommands and take appropriate actions depending on the TransmissionType and Argument.
	 * 
	 * @param transmission	the transmission that was just received and should be checked for keywords in the header.
	 */
	private void processMessage(NetworkPackage transmission) {

		System.out.println("[" + connectionID + "]: Received Message, starting processing!: " + transmission.getHead() + " - " + transmission.getTypeArg() + " - " + transmission.getContent());
		//Chose processing based on transmissionType in the NetworkPackage head.
		switch(transmission.getHead()){
		
		case CONNECTION_CONFIRMATION:
			System.out.println("[" + connectionID + "]: Connection Confirmation received!");
			remoteName = transmission.getTypeArg();
			isBuildingConnection = false;
			isConnected = true;
			return;
			
		case CONNECTION_TERMINATION:	//This is received if the other, connected connectionEndpoint wishes to close the connection. Takes all necessary actions on this local side of the connection.
			//System.out.println("[" + connectionID + "]: TerminationOrder Received at " + connectionID + "!");
			closeConnection(false);
			return;
			
		case TRANSMISSION:	//This is received if the connected connectionEndpoint wants to send this CE a transmission containing actual data in the NetworkPackages content field. The transmission is added to the MessageStack.
			receiveMessage(transmission);
			return;
			
		case FILE_TRANSFER:
			receiveFile(transmission);
			return;
			
		case RECEPTION_CONFIRMATION_REQUEST:	//This works similar to the regular Transmission but it indicates the sender is waiting for a reception confirmation. This sends this confirmation back.
			//System.out.println("[" + connectionID + "]: Received Confirm-Message: " + transmission.getHead() + "!");
			receiveMessage(transmission);
			pushMessage(TransmissionTypeEnum.RECEPTION_CONFIRMATION_RESPONSE, transmission.getTypeArg(), null, null);
			return;
			
		case RECEPTION_CONFIRMATION_RESPONSE:	//This is received if the local CE has sent a confirmedMessage and is waiting for the confirmation. Once received the confirmation in the form of the messageID is added to the pendingConfirmations.
			//System.out.println("[" + connectionID + "]: Received Confirm_Back-Message: " + transmission.getHead() + "!");
			registerConfirmation(transmission.getTypeArg());
			return;
			
		case KEYGEN_SYNC_REQUEST:	//This is received if another ConnectionEndpoint that is connected to this one is intending to start a KeyGeneration Process and is asking for a response(accept/reject).
			//System.out.println("[" + connectionID + "]: Received KeyGenSync-Message: " + transmission.getHead() + "!");
			SHA256withRSAAuthentication authenticator = new SHA256withRSAAuthentication();
			if (authenticator.verify(transmission.getContent(), transmission.getSignature(), connectionID)) {
				keyGen.keyGenSyncResponse();
			}
			return;
			
		case KEYGEN_SYNC_ACCEPT:	//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncConfirm is added to the regular messagesStack and read by the KeyGenerator.
			//System.out.println("[" + connectionID + "]: Received KeyGenSyncResponse-Message: " + transmission.getHead() + "!");
			//addMessageToQueue( transmission);
			keyGen.updateAccRejState(1);
			return;
			
		case KEYGEN_SYNC_REJECT:	//This is received as a response to a KEYGEN_SYNC_REQUEST. It signals to this ConnectionEndpoint that the sender is willing to start the KeyGen Process.
			//The SyncReject is added to the regular messagesStack and read by the KeyGenerator.
			//System.out.println("[" + connectionID + "]: Received KeyGenSyncResponse-Message: " + transmission.getHead() + "!");
			//addMessageToQueue( transmission);
			keyGen.updateAccRejState(-1);
			return;		
			
		case KEYGEN_TRANSMISSION:
			keyGen.writeKeyGenFile(transmission);
			return;
			
		case KEYGEN_SOURCE_SIGNAL:	//This is only used for signaling the source server to start sending photons. 
			//TODO: Add source logic. It just needs to drop a file containing the message content as Text in a special folder where the source is waiting for the file.
			return;
			
		case KEYGEN_TERMINATION:	//This is received if the connected ConnectionEndpoint intends to terminate the KeyGen Process. This will cause a local shutdown in response.
			//Terminating Key Gen
			keyGen.shutdownKeyGen(false, true);
			return;
			
		default:	//This is the fallback if no valid Transmission Type was recognized.
			System.err.println("ERROR: [" + connectionID + "]: Invalid message prefix in message: " + transmission.getHead());
			return;
		
		
}
	}

	
	/**This is the parallel running task that accepts and handles newly received messages.
	 * This is usually started by listenForMessage().
	 * 
	 */
	@Override
	public void run() {
		waitingForMessage = true;
		NetworkPackage receivedMessage;
		while(waitingForMessage) {
			try {
				if(waitingForMessage && clientIn != null && (isConnected||isBuildingConnection) && (receivedMessage = (NetworkPackage) clientIn.readObject()) != null) {
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
		listenForMessages = false;
	}
	
	/**This Method handles all types of Transmissions.
	 * TRANSMISSION, FILE_TRANSFER and RECEPTION_CONFIRMATION_REQUEST will be handled by this method after being processed.
	 * 
	 * @param transmission the new transmission.
	 */
	private void receiveMessage(NetworkPackage transmission) {
		String msg;
		System.out.println("[" + connectionID + "]: Receiving new Message...");
		
		if(transmission.getHead() == TransmissionTypeEnum.FILE_TRANSFER) {
			//TODO: Implement File Handling
		}
		
		
		if(transmission.getTypeArg().split(":::")[0] == "encrypted") {
			//Handle Encrypted Messages
			msg = MessageSystem.readEncryptedMessage(connectionID, transmission);
			if(msg == null) {
				System.err.println("ERROR: Could not Authenticate Message: " + MessageSystem.byteArrayToString(MessageSystem.readMessage(transmission)));
				new GenericWarningMessage("ERROR: Could not Authenticate Message: " + MessageSystem.byteArrayToString(MessageSystem.readMessage(transmission)));
			}
			return;
		}
		
		if(transmission.getSignature() != null) {
			//Handle Authenticated Messages
			msg = MessageSystem.byteArrayToString(MessageSystem.readAuthenticatedMessage(connectionID, transmission));
			if(msg == null) {
				System.err.println("ERROR: Could not Authenticate Message: " + MessageSystem.byteArrayToString(MessageSystem.readMessage(transmission)));
				new GenericWarningMessage("ERROR: Could not Authenticate Message: " + MessageSystem.byteArrayToString(MessageSystem.readMessage(transmission)));
			}
			
		}else {
			//Handle Unsafe Messages
			msg = MessageSystem.byteArrayToString(MessageSystem.readMessage(transmission));
		}
		
		//Update Message Log
		logMessage(messageLog + "\n" + remoteName +" wrote: \n" + msg);
		
		if(logGUI != null) {
			logGUI.refreshMessageLog();
		}
	}
	
	private void receiveFile(NetworkPackage transmission) {
		//TODO: Add creation of file here! I assume the Argument of the NetworkPackage will contain the path + filename.filetype!
	}
	
	/**This replaces the message log with a new one.
	 * Normally the new one also contains the old one.
	 * 
	 * @param msg the new message log.
	 */
	public void logMessage(String msg) {
		messageLog = (msg);
	}
	
	/**This returns the MessageLog that contains any plain-text messages of this connection.
	 * 
	 * @return the message log
	 */
	public String getMessageLog() {
		return messageLog;
	}

}
