package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;

public class ConnectionSwitchbox implements Runnable{
	
	private boolean acceptingConnections = false;
	private int port = 0000;
	private ServerSocket masterServerSocket;
	private Socket newClientSocket;
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();
	private Map<String,ObjectInputStream> serverInputs = new HashMap<String,ObjectInputStream>();
	private Map<String,ConnectionEndpointServerHandler> serverThreads = new HashMap<String,ConnectionEndpointServerHandler>();
	
	public ConnectionSwitchbox(int portnumber) {
		port = portnumber;
		
		//Create Master Socket
		try {
			masterServerSocket = new ServerSocket(port);
			waitForConnection();
		} catch (IOException e) {
			System.err.println("Could not create MasterServerSocket @ Port " + port);
			e.printStackTrace();
		}

	}
	
	public ServerSocket getMasterServerSocket() {
		return masterServerSocket;
	}
	
	public int getMasterServerPort() {
		return port;
	}
	
	public void waitForConnection() {
		System.out.println("Preparing to accept incoming Connections on Port "+ port +"...");
		acceptingConnections = true;
		connectionExecutor.submit(() -> {
			while(acceptingConnections) {	
			
				//Try accepting ConnectionRequest;
				try {
					//Listen for connection attempt
					System.out.println("Waiting to accept incoming Connections on Port "+ port +"...");
					ConnectionEndpointServerHandler cESH = new ConnectionEndpointServerHandler(newClientSocket = masterServerSocket.accept());
					cESH.start();
					serverThreads.put("tmpName" + MessageSystem.generateRandomMessageID(), cESH);
					System.out.println("---MasterServer has received a connection attempt!---");
					System.out.println("---MasterServer is adding new InputStream.---");
				
					
			
				
				} catch (IOException e) {
					System.err.println("Server of ConnectionSwitchbox failed to accept connection attempt!");
					e.printStackTrace();
				}
			}
		});
		
	}


	public void stopAcceptingConnections() {
		acceptingConnections = false;
		try {
			masterServerSocket.close();
		} catch (IOException e) {
			System.err.println("There was an issue in the Switchbox while closing the masterServerSocket!");
			e.printStackTrace();
		}
		connectionExecutor.shutdownNow();
	}
	

	@Override
	public void run() {
		/**
		//Read incoming Messages for the ServerStream and enqueue them on the correct CEs MessageStack.
		ObjectOutputStream serverOut = new ObjectOutputStream(masterServerSocket.getOutputStream());
		ObjectInputStream serverIn = new ObjectInputStream(masterServerSocket.getInputStream());
		
		while(true) {
			//masterServerSocket
		}
		*//
	}
}
