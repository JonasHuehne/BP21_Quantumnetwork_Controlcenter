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

public class ConnectionSwitchbox {
	
	private boolean acceptingConnections = false;
	private int port = 0000;
	private ServerSocket masterServerSocket;
	private Socket newClientSocket;
	private ExecutorService connectionExecutor = Executors.newSingleThreadExecutor();

	
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
					//Listen for connection attempt and create new Handler once one was accepted.
					new ConnectionEndpointServerHandler(newClientSocket = masterServerSocket.accept(), QuantumnetworkControllcenter.conMan.getLocalAddress(), QuantumnetworkControllcenter.conMan.getLocalPort()).start();
				
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
}
