import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;

/**
 * This is a messy test class to try out some things regarding Socket behaviour.
 * @author Sasha Petri
 */
public class WeirdNetworkBehaviourTests {

	/** The ConnectionManager, used for some tests, specifically regarding the connection commands */
	private static ConnectionManager conMan;
	
	@BeforeAll
	static void initialize() {
		QuantumnetworkControllcenter.initialize();
		conMan	 = QuantumnetworkControllcenter.conMan;
	}
	
	@Test
	void send_request_to_CE_that_is_not_waiting() {
		/*
		 * This blocks indefinitely, which is weird.
		 * Shouldn't "localClientSocket = new Socket(remoteID,remotePort);"
		 * in ConnectionEndpoint throw an Exception, because 
		 */
		ConnectionEndpoint BobToAlice = conMan.createNewConnectionEndpoint("Alice", 50001); // Endpoint of Bob, connected to Alice
		ConnectionEndpoint AliceToBob = conMan.createNewConnectionEndpoint("Bob", 50002); // Endpoint of Alice, connected to Bob
		
		try {
			AliceToBob.establishConnection("localhost", 50001);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Test
	void generic_socket_behaviour_testing() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(50010);
			server.setSoTimeout(1000);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Socket s = new Socket();
		
		try {
			
			System.out.println("Client is connected to: " + s.getInetAddress());
			System.out.println("Server is connected to: " + server.getLocalSocketAddress());
			
			try {
				Socket serverClientSocket = server.accept();
				assertTrue(false, "Something went wrong, accept should have not accepted any request.");
			} catch (SocketTimeoutException t) {
				System.out.println("Server did not find any connection request to accept.");
				System.out.println("Server is NOT running accept anymore.");
			}
			
			System.out.println("--- Client is attempting connection now ---");
			s.connect(new InetSocketAddress("127.0.0.1", 50010), 1000);
			System.out.println("Client is connected? " + s.isConnected());
			System.out.println("Client is connected to: " + s.getInetAddress());
			System.out.println("Server is bound to (LocalSocketAddress): " + server.getLocalSocketAddress());
			System.out.println("Local Address of the Server (InetAddress): " + server.getInetAddress());
			
			System.out.println("------ Now, we run accept on server again. ------");
			
			Socket serverClientSocket = server.accept();
			
			System.out.println("Nothing went wrong, a connection could be established with the queued request.");
			System.out.println("Server is bound to (LocalSocketAddress): " + server.getLocalSocketAddress());
			System.out.println("Local Address of the Server (InetAddress): " + server.getInetAddress());
			System.out.println("ServerClientSocket.getInetAddress() = " + serverClientSocket.getInetAddress());
			
			serverClientSocket.close();
			s.close();
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	/*
	 * It appears that there simply being a ServerSocket with the port
	 * that we are trying to connect to is enough for the Socket to connect.
	 * The ServerSocket does not need to run .accept() for that.
	 * However, for the server, it seems the connection state does not change before
	 * or after the client socket runs anything?
	 */
}
