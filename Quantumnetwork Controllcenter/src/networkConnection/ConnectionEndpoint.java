package networkConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionEndpoint implements Runnable{
	
	//Local information
	private ConnectionManager owningConnectionManager;
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
	Thread messageThread = new Thread(this, connectionID + "_messageThread");
	
	public ConnectionEndpoint(ConnectionManager cm, String connectionName, String localAddress, int serverPort) {
		owningConnectionManager = cm;
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
			return ConnectionState.Connected;
		}
		if(!isConnected && !isBuildingConnection && !waitingForConnection && !waitingForMessage) {
			return ConnectionState.Closed;
		}
		if(waitingForConnection && !isConnected && !isBuildingConnection) {
			return ConnectionState.WaitingForConnection;
		}
		if(!waitingForConnection && !isConnected && !isBuildingConnection && waitingForMessage) {
			return ConnectionState.WaitingForMessage;
		}
		if(!waitingForConnection && !isConnected && isBuildingConnection) {
			return ConnectionState.Connecting;
		}
		return ConnectionState.ERROR;
	}
	
	public void updateLocalAddress(String newLocalAddress) {
		localAddress = newLocalAddress;
	}
	//------------//
	//Client Side
	//------------//
	
	/**Tries to connect to another Endpoints ServerSocket
	 * 
	 * @param targetServerIP	the IP or Name of the other Party.
	 * @param targetServerPort	the Port on which the other Partys ServerSocket is listening for connections.
	 */
	public void EstablishConnection(String targetServerIP, int targetServerPort) {
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
			e.printStackTrace();
		}
		
	}
	
	/**Closes the connection to another Endpoint
	 * 
	 * @throws IOException	may complain if something goes wrong, handle above.
	 */
	public void closeConnection() throws IOException {
		System.out.println("[" + connectionID + "]: Local Shutdown of ConnectionEndpoint " + connectionID);
		isConnected = false;
		isBuildingConnection = false;
		waitingForConnection = false;
		waitingForMessage = false;
		listenForMessages = false;
		if(localServerSocket != null) {
			localServerSocket.close();
			serverIn = null;
			localServerSocket = null;
		}
		if(localClientSocket != null) {
			localClientSocket.close();
			clientOut = null;
			localClientSocket = null;
		}
		if(remoteClientSocket != null) {
			remoteClientSocket.close();
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
					System.out.println("[" + connectionID + "]: A ConnectionRequest has been recieved at " + connectionID + "s ServerSocket on Port " + localServerPort + "!");
					
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
	 * @return	returns the String of the next recieved message.
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
	private String ProcessMessage(String message) {
		//Closing Message
		System.out.println("[" + connectionID + "]: Processing Message: " + message);
		try {
			switch(message.split(":::")[0]){
			case "connreq":
				//Parse Greeting
				String greetingMessage = message.split(":::")[1];
				remoteID = greetingMessage.split(":")[0];
				remotePort = Integer.parseInt(greetingMessage.split(":")[1]);
				System.out.println("[" + connectionID + "]: Recieved initial Message: " + greetingMessage);
			
				//Use greeting(ip:port) to establish back-connection to the ConnectionAttempt-Sources ServerSocket
				System.out.println("[" + connectionID + "]: Connecting back to " + remoteID + " at Port: " + remotePort);
				localClientSocket = new Socket(remoteID, remotePort);
				clientOut = new PrintWriter(localClientSocket.getOutputStream(), true);
				return "";
			case "termconn":
				System.out.println("[" + connectionID + "]: TerminationOrder Recieved at " + connectionID + "!");
				closeConnection();
				return message;
			case "confirm":
				System.out.println("[" + connectionID + "]: Recieved Confirm-Message: " + message + "!");
				pushMessage("confirmback:::" + message.split(":::")[1]);
				return message.split(":::")[1];
			case "confirmback":
				System.out.println("[" + connectionID + "]: Recieved Confirm_Back-Message: " + message + "!");
				return "";
			
			
		}
		
		} catch (IOException e) {
			System.out.println("There was an issue at " + connectionID + " while tring to parse special commands from the latest Message: " + message + ".");
			e.printStackTrace();
		}
				
		return message;
	}

	@Override
	public void run() {
		waitingForMessage = true;
		String recievedMessage;
		while(waitingForMessage) {
			try {
				if(waitingForMessage && serverIn != null && isConnected && !waitingForConnection && (recievedMessage = serverIn.readLine()) != null) {
					System.out.println("[" + connectionID + "]: " + connectionID + " recieved Message!:");
					System.out.println(recievedMessage);
					ProcessMessage(recievedMessage);

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
