package networkConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionEndpoint implements Runnable{
	
	//Local information
	private String connectionID;
	
	//Addresses, Sockets and Ports
	private String localAddress;	//the own IP
	private ServerSocket localServerSocket; // our own Endpoint that listens on a port and waits for a connection Request from another Endpoints ClientSocket.
	private int localServerPort;	//the Port of the local Server that other Endpoints can send Messages to.
	private Socket localClientSocket;	// connects to another Endpoints Serversocket.
	private Socket remoteClientSocket;	// another Endpoints ClientSocket that messaged out local Server Socket	
	private String remoteID;	//the IP of the connected Endpoint
	private int remotePort;		//the Port of the connected Endpoint
	
	//Communication Channels
	private BufferedReader serverIn;
	private PrintWriter clientOut;
	
	//State
	private boolean listenForMessages = false;
	private boolean isConnected = false;
	private boolean isBuildingConnection = false;
	private boolean waitingForConnection = false;
	private boolean waitingForMessage = false;
	
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	private Thread messageThread = new Thread(this, connectionID + "_messageThread");
	
	private LinkedList<String> messageStack = new LinkedList<String>();
	private LinkedList<String> confirmedMessageStack = new LinkedList<String>();
	
	public ConnectionEndpoint(String connectionName, String localAddress, int serverPort) {
		connectionID = connectionName;
		this.localAddress = localAddress;
		localServerPort = serverPort;
		try {
			localServerSocket = new ServerSocket(localServerPort);
			System.out.println("Initialised local ServerSocket in Endpoint of " + connectionID + " at Port " + String.valueOf(localServerPort));
		} catch (IOException e) {
			System.out.println("Local Connection Endpoint could not be established! An Error occured while creating the local Client and Server Sockets!");
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
		if(!isConnected && !isBuildingConnection && !waitingForConnection && !waitingForMessage) {
			return ConnectionState.CLOSED;
		}
		if(waitingForConnection && !isConnected && !isBuildingConnection) {
			return ConnectionState.WAITINGFORCONNECTION;
		}
		if(!waitingForConnection && !isConnected && !isBuildingConnection && waitingForMessage) {
			return ConnectionState.WAITINGFORMESSAGE;
		}
		if(!waitingForConnection && !isConnected && isBuildingConnection) {
			return ConnectionState.CONNECTING;
		}
		return ConnectionState.ERROR;
	}
	
	/**updated the local IP
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
	//------------//
	//Client Side
	//------------//
	
	/**Tries to connect to another Endpoints ServerSocket
	 * 
	 * @param targetServerIP	the IP or Name of the other Party.
	 * @param targetServerPort	the Port on which the other Partys ServerSocket is listening for connections.
	 * @throws IOException 
	 */
	public void establishConnection(String targetServerIP, int targetServerPort) throws IOException {
		if(isConnected) {
			System.out.println("Warning: " + connectionID + " is already connected to " + remoteID + " at Port " + String.valueOf(remotePort) + "! Connection creation aborted!");
			return;
		}
		System.out.println("[" + connectionID + "]: Attempting to connect " + connectionID + " to: " + targetServerIP + " on port " + String.valueOf(targetServerPort) + "!");

		isBuildingConnection = true;
		remoteID = targetServerIP;
		remotePort = targetServerPort;
		
		//Try to connect to other Server
		try {
			//Connecting own Client Socket to foreign Server Socket
			localClientSocket = new Socket(remoteID,remotePort);
			clientOut = new PrintWriter(localClientSocket.getOutputStream(),true);
			isConnected = true;
			
			//Send Message to allow foreign Endpoint to connect with us.
			System.out.println("[" + connectionID + "]: " + connectionID + " is sending a greeting.");
			pushMessage("connreq:::" + localAddress + ":" + localServerPort);
			System.out.println("[" + connectionID + "]: waiting for response");
			remoteClientSocket = localServerSocket.accept();
			serverIn = new BufferedReader(new InputStreamReader(remoteClientSocket.getInputStream()));
			System.out.println("[" + connectionID + "]: Connected " + connectionID + " to external ID and Port: " + remoteID + ", " + String.valueOf(remotePort) + "!");
			isBuildingConnection = false;
			listenForMessage();
			
		//Error Messages
		} catch (UnknownHostException e) {
			isConnected = false;
			System.out.println("[" + connectionID + "]: Connection could not be established! An Error occured while trying to reach the other party via " + remoteID + ":" + String.valueOf(remotePort));
			e.printStackTrace();
		} catch (IOException e) {
			isConnected = false;
			System.out.println("[" + connectionID + "]: Connection could not be established! An Error occured while connecting to the other Client!");
			throw e;
		}
		
	}
	
	
	/**Closes the connection to another Endpoint
	 * 
	 * @param localRequest true if the request to close the connection has a local origin, false if the request came as a message from the other Endpoint.
	 * @throws IOException	may complain if something goes wrong, handle above.
	 */
	public void closeConnection(boolean localRequest) {
		if(isConnected && localRequest) {
			//If close-request has local origin, message other connectionEndpoint about closing the connection.
			pushMessage("termconn:::");
		}
		System.out.println("[" + connectionID + "]: Local Shutdown of ConnectionEndpoint " + connectionID);
		isConnected = false;
		isBuildingConnection = false;
		waitingForConnection = false;
		waitingForMessage = false;
		listenForMessages = false;
		if(localServerSocket != null) {
			try {
				localServerSocket.close();
			} catch (IOException e) {
				System.out.println("[" + connectionID + "]: Shutdown of localServerSocket failed!");
				e.printStackTrace();
			}
			serverIn = null;
			localServerSocket = null;
		}
		if(localClientSocket != null) {
			try {
				localClientSocket.close();
			} catch (IOException e) {
				System.out.println("[" + connectionID + "]: Shutdown of localClientSocket failed!");
				e.printStackTrace();
			}
			clientOut = null;
			localClientSocket = null;
		}
		if(remoteClientSocket != null) {
			try {
				remoteClientSocket.close();
			} catch (IOException e) {
				System.out.println("[" + connectionID + "]: Shutdown of remoteClientSocket failed!");
				e.printStackTrace();
			}
			remoteClientSocket = null;
		}		
		if(!connectionExecutor.isShutdown()) {
			connectionExecutor.shutdownNow();
		}
	}
	
	/**Pushes a Message to the connected Endpoints ServerSocket.
	 * 
	 * @param message the String Message that should be send to the connected Partys Server.
	 */
	public void pushMessage(String message) {
		//Check for existence of connection before attempting so send.
		if(!isConnected) {
			System.out.println("Warning: Attempted to push a message to another Endpoint while not beeing connected to anything!");
			return;
		}
		System.out.println("[" + connectionID + "]: ConnectionEndpoint of " + connectionID + " is pushing Message: " + message + " to ServerSocket of " + remoteID + ":" + String.valueOf(remotePort) + "!");
		clientOut.println(message);
		return;
	}
	
	public void addMessageToStack(String message) {
		messageStack.add(message);
	}

	public String readMessageFromStack() {
		if (messageStack.size()>0) {
			return messageStack.pop();
		}
		return "";
	}
	
	public String peekMessageFromStack() {
		if (messageStack.size()>0) {
			return messageStack.peekFirst();
		}
		return "";
	}
	
	public String peekLatestMessageFromStack() {
		if (messageStack.size()>0) {
		return messageStack.peekLast();
		}
		return "";
	}
	
	public int sizeOfMessageStack() {
		return messageStack.size();
	}
	
	private void registerConfirmation(String message) {
		confirmedMessageStack.add(message);
	}
	
	public LinkedList<String> getConfirmations(){
		return confirmedMessageStack;
	}
	
	public void clearConfirmation(String conf){
		confirmedMessageStack.remove(conf);
	}
	
	@SuppressWarnings("unchecked")
	public LinkedList<String> getMessageStack(){
		return (LinkedList<String>) messageStack.clone();
	}
	//------------//
	//Server Side
	//------------//
	
	
	/**Loops non-blocking until a connection has been attempted from the outside.
	 * Then it helps establish a two-way connection to the outside party.
	 */
	public void waitForConnection() {
		connectionExecutor.submit(() -> {
			System.out.println("[" + connectionID + "]: " + connectionID + " is beginning to wait for a ConnectionAttempt from the outside on Port " + localServerPort + "!");
			waitingForConnection = true;
			while(waitingForConnection && !isConnected && !isBuildingConnection) {	
			
				//Try accepting ConnectionRequest;
				try {
					//Listen for connection attempt
					remoteClientSocket = localServerSocket.accept();
					System.out.println("[" + connectionID + "]: A ConnectionRequest has been received at " + connectionID + "s ServerSocket on Port " + localServerPort + "!");
					
					//Set ServerCommmChannels
					serverIn = new BufferedReader(new InputStreamReader(remoteClientSocket.getInputStream()));
					waitingForConnection = false;
					isConnected = true;
					
					//Wait for greeting
					System.out.println("[" + connectionID + "]: Wating for Greeting from connecting Party");
					listenForMessage();
					
				
				
				
				} catch (IOException e) {
					System.out.println("Server of ConnectionEndpoint " + connectionID + " failed to accept connection attempt!");
					e.printStackTrace();
				}
			}
			connectionExecutor.shutdown();
		});
		connectionExecutor.shutdown();
	}
		
	/**Waits until a message was received and then returns the message. Blocking.
	 * 
	 * @return	returns the String of the next received message.
	 */
	public void listenForMessage() {
		if(listenForMessages) {
			System.out.println("[" + connectionID + "]: Already listening for Message, not starting a 2. Thread.");
			return;
		}
		listenForMessages = true;
		System.out.println("[" + connectionID + "]: Waiting for Message has startet!");
		messageThread.start();
		return;			
	}
	
	/**PreProcessing-step to filter out ConnectionCommands
	 * 
	 * @param message	the message that was just received and should be checked for keywords.
	 * @return	the String message after it was processed.
	 */
	private void processMessage(String message) {
		//Closing Message
		System.out.println("[" + connectionID + "]: Processing Message: " + message);
		try {
			switch(message.split(":::")[0]){
			
			case "connreq":
				//Parse Greeting
				String greetingMessage = message.split(":::")[1];
				remoteID = greetingMessage.split(":")[0];
				remotePort = Integer.parseInt(greetingMessage.split(":")[1]);
				System.out.println("[" + connectionID + "]: Received initial Message: " + greetingMessage);
			
				//Use greeting(ip:port) to establish back-connection to the ConnectionAttempt-Sources ServerSocket
				System.out.println("[" + connectionID + "]: Connecting back to " + remoteID + " at Port: " + remotePort);
				localClientSocket = new Socket(remoteID, remotePort);
				clientOut = new PrintWriter(localClientSocket.getOutputStream(), true);
				return;
				
			case "termconn":
				System.out.println("[" + connectionID + "]: TerminationOrder Received at " + connectionID + "!");
				closeConnection(false);
				return;
				
			case "msg":
				System.out.println("[" + connectionID + "]: Received Message: " + message + "!");
				addMessageToStack( message.split(":::")[1]);
				return;
				
			case "confirm":
				System.out.println("[" + connectionID + "]: Received Confirm-Message: " + message + "!");
				addMessageToStack( message.split(":::")[1]);
				pushMessage("confirmback:::" + message.split(":::")[1]);
				return;
				
			case "confirmback":
				System.out.println("[" + connectionID + "]: Received Confirm_Back-Message: " + message + "!");
				registerConfirmation(message.split(":::")[1]);
				return;
				
			default:
				System.out.println("ERROR: [" + connectionID + "]: Invalid message prefix in message: " + message);
				return;
			
			
		}
		
		} catch (IOException e) {
			System.out.println("There was an issue at " + connectionID + " while tring to parse special commands from the latest Message: " + message + ".");
			e.printStackTrace();
		}
				
		return;
	}

	@Override
	public void run() {
		waitingForMessage = true;
		String receivedMessage;
		while(waitingForMessage) {
			try {
				if(waitingForMessage && serverIn != null && isConnected && !waitingForConnection && (receivedMessage = serverIn.readLine()) != null) {
					System.out.println("[" + connectionID + "]: " + connectionID + " received Message!:");
					System.out.println(receivedMessage);
					processMessage(receivedMessage);

				}
			} catch (IOException e) {
				if(isConnected) {
					System.out.println("Error while waiting for Message at " + connectionID + "!");
					e.printStackTrace();
				}
			}
		}
		listenForMessages = false;
	}

}
