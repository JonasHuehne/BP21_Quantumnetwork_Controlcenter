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

public class ConnectionEndpoint {
	
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
	private boolean isConnected = false;
	private boolean isBuildingConnection = false;
	private boolean waitingForConnection = false;
	private boolean waitingForMessage = false;
	
	String lastMessage = null;
	
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	
	public ConnectionEndpoint(ConnectionManager cm, String connectionName, String localAddress, int serverPort) {
		owningConnectionManager = cm;
		connectionID = connectionName;
		this.localAddress = localAddress;
		localServerPort = serverPort;
		try {
			//localClientSocket = new Socket("127.0.0.1",localClientPort);
			localServerSocket = new ServerSocket(localServerPort);
			System.out.println("Initialised local ServerSocket in Endpoint of " + connectionID + " at Port " + String.valueOf(localServerPort));
		} catch (IOException e) {
			System.out.println("Local Connection Endpoint could not be established! An Error occured while creating the local Client and Server Sockets!");
			e.printStackTrace();
		}
	}
	
	//Reports the current State of this Endpoints Connection.
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
	
	
	//------------//
	//Client Side
	//------------//
	
	//Tries to connect to another Endpoints ServerSocket
	public void EstablishConnection(String targetServerIP, int targetServerPort) {
		if(isConnected) {
			System.out.println("Warning: " + connectionID + " is already connected to " + remoteID + " at Port " + String.valueOf(remotePort) + "! Connection creation aborted!");
		}
		System.out.println("Attempting to connect " + connectionID + " to: " + targetServerIP + " on port " + String.valueOf(targetServerPort) + "!");

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
			System.out.println(connectionID + " is sending a greeting.");
			pushMessage(localAddress + ":" + localServerPort);
			System.out.println("waiting for response");
			remoteClientSocket = localServerSocket.accept();
			serverIn = new BufferedReader(new InputStreamReader(remoteClientSocket.getInputStream()));
			System.out.println("Connected " + connectionID + " to external ID and Port: " + remoteID + ", " + String.valueOf(remotePort) + "!");
			isBuildingConnection = false;
			
		//Error Messages
		} catch (UnknownHostException e) {
			isConnected = false;
			System.out.println("Connection could not be established! An Error occured while trying to reach the other party via " + remoteID + ":" + String.valueOf(remotePort));
			e.printStackTrace();
		} catch (IOException e) {
			isConnected = false;
			System.out.println("Connection could not be established! An Error occured while connecting to the other Client!");
			e.printStackTrace();
		}
		
	}
	
	
	//Closes the connection to another Endpoint
	public void closeConnection() throws IOException {
		System.out.println("Local Shutdown of ConnectionEndpoint " + connectionID);
			localServerSocket.close();
			localClientSocket.close();
			remoteClientSocket.close();
			isConnected = false;
			isBuildingConnection = false;
			waitingForConnection = false;
			waitingForMessage = false;
			if(!connectionExecutor.isShutdown()) {
				connectionExecutor.shutdownNow();
			}
	}
	
	//Pushes a Message to the connected Endpoints ServerSocket.
	public void pushMessage(String message) {
		System.out.println("Attempting to push message: " + message);
		//Check for existence of connection before attempting so send.
		if(!isConnected) {
			System.out.println("ERROR: Attempted to push a message to another Endpoint while not beeing connected to anything!");
			return;
		}
		System.out.println("ConnectionEndpoint of " + connectionID + " is pushing Message: " + message + " to ServerSocket of " + remoteID + ":" + String.valueOf(remotePort) + "!");
		clientOut.println(message);
		return;
	}
	
	
	//------------//
	//Server Side
	//------------//
	
	
	//Loops non-blocking until a connection has been attempted from the outside.
	public void waitForConnection() {
		connectionExecutor.submit(() -> {
			System.out.println(connectionID + " is beginning to wait for a ConnectionAttempt from the outside on Port " + localServerPort + "!");
			waitingForConnection = true;
			while(waitingForConnection && !isConnected && !isBuildingConnection) {	
			
				//Try accepting ConnectionRequest;
				try {
					//Listen for connection attempt
					remoteClientSocket = localServerSocket.accept();
					System.out.println("A ConnectionRequest has been recieved at " + connectionID + "s ServerSocket on Port " + localServerPort + "!");
					
					//Set ServerCommmChannels
					//serverOut = new PrintWriter(remoteClientSocket.getOutputStream(),true);
					serverIn = new BufferedReader(new InputStreamReader(remoteClientSocket.getInputStream()));
					waitingForConnection = false;
					isConnected = true;
					
					//Wait for greeting
					System.out.println("Wating for Greeting from connecting Party");
					listenForMessage();
					while(lastMessage == null) {
						try {
							System.out.println("!!!!!!!!!!!!!HAD TO WAIT!!!!!!!!!!!!");
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					String greetingMessage = lastMessage;
					lastMessage = null;
					remoteID = greetingMessage.split(":")[0];
					remotePort = Integer.parseInt(greetingMessage.split(":")[1]);
					System.out.println("Recieved initial Message: " + greetingMessage);
				
					//Use greeting(ip:port) to establish back-connection to the ConnectionAttempt-Sources ServerSocket
					System.out.println("Connecting back to " + greetingMessage.split(":")[0] + " at Port: " + greetingMessage.split(":")[1]);
					localClientSocket = new Socket(greetingMessage.split(":")[0], Integer.parseInt(greetingMessage.split(":")[1]));
					clientOut = new PrintWriter(localClientSocket.getOutputStream(), true);
				
				
				
				} catch (IOException e) {
					System.out.println("Server of ConnectionEndpoint " + connectionID + " failed to accept connection attempt!");
					e.printStackTrace();
				}
			}
			connectionExecutor.shutdown();
		});
		connectionExecutor.shutdown();
	}
	
	public void listenForMessage() {
		System.out.println("Waiting for Message has startet!");
		waitingForMessage = true;
		String recievedMessage;
		while(waitingForMessage) {
			try {
				if(isConnected && !waitingForConnection && (recievedMessage = serverIn.readLine()) != null) {
					System.out.println(connectionID + " recieved Message!:");
					System.out.println(recievedMessage);
					waitingForMessage = false;
					lastMessage = recievedMessage;
					return ;//preProcessMessage(recievedMessage);
				}
			} catch (IOException e) {
				System.out.println("Error while waiting for Message at " + connectionID + "!");
				e.printStackTrace();
			}
		}
		return ;//"No message recived!";			
	}
	
	//PreProcessing-step to filter out ConnectionCommands
	private String preProcessMessage(String message) {
		//Closing Message
		if(message.split(":")[0].equals("TerminateConnection")) {
			try {
				System.out.println("TerminationOrder Recieved at " + connectionID + "!");
				closeConnection();
			} catch (IOException e) {
				System.out.println("There was an issue at " + connectionID + " while tring to close down the current Connection after recieving the TerminateConnection-Message.");
				e.printStackTrace();
			}
		}		
		return message;
	}

}
