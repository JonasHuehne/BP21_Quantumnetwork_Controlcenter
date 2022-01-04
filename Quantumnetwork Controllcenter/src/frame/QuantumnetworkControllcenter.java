package frame;

import networkConnection.ConnectionManager;
import ui.ConsoleUI;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import MessengerSystem.MessageSystem;

/*
 * Main Class of QuantumnetworkControllcenter
 */
public class QuantumnetworkControllcenter {
	
	public static ConnectionManager conMan;
	/*
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public static void initialize() {
		
		//TODO add initialization of further Classes
		
		//Network Connection Init
		String localIP = "127.0.0.1"; //Must be changed manually as of right now. Use IP depending on intended communication Range (local Machine, local Network or Internet)
		conMan = new ConnectionManager(localIP);
		MessageSystem.conMan = conMan;
		
		System.out.println("QuantumnetworkControllcenter initialized");
	}
	 
	
	/*
	 * Main-method to run QuantumnetworkControllcenter
	 */
	public static void main(String[] args) {
		

		
		System.out.println("Run QuantumnetworkControllCenter initialisation");
		
		initialize();
		
		// Launch Console UI
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConsoleUI window = new ConsoleUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		
		System.out.println("--- Connecting Alice and Bob! ---");
		
		conMan.createNewConnectionEndpoint("Alice", 2303);
		conMan.createNewConnectionEndpoint("Bob", 3303);
		
		System.out.println("State of Alice: " + conMan.getConnectionEndpoint("Alice").reportState());
		System.out.println("State of Bob: " + conMan.getConnectionEndpoint("Bob").reportState());
		
		conMan.getConnectionEndpoint("Bob").waitForConnection();
		try {
			conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3303);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("State of Alice: " + conMan.getConnectionEndpoint("Alice").reportState());
		System.out.println("State of Bob: " + conMan.getConnectionEndpoint("Bob").reportState());
		
		System.out.println("--- Connection between Alice and Bob established! ---");
		
		MessageSystem.setActiveConnection("Bob");
		System.out.println("Confirmed Message Transmission Success: " + MessageSystem.sendConfirmedMessage("Confirm me plz"));
		
		
		/**System.out.println("--- Testing low-level comm ---");
		conMan.sendMessage("Alice", "TestNachricht");
		conMan.sendMessage("Alice", "TestNachricht2");
		conMan.sendMessage("Bob", "TestNachricht4");
		conMan.sendMessage("Alice", "TestNachricht3");
		conMan.sendMessage("Bob", "TestNachricht5");
		conMan.sendMessage("Bob", "TestNachricht6");
		
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("--- End of low-level comm Test ---");
		System.out.println("--- High-level comm Test ---");
		MessageSystem.setActiveConnection("Alice");
		MessageSystem.sendConfirmedMessage("abc123 neue confirmed Message plzzz");
		
		MessageSystem.setActiveConnection("Bob");		
		MessageSystem.sendConfirmedMessage("kek ok i haz cofirmed, can u read 2??");
		
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("--- End of high-level comm Test ---");
		
		MessageSystem.setActiveConnection("Alice");
		MessageSystem.sendMessage("NormaleNachricht");
		MessageSystem.setActiveConnection("Bob");
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		/**System.out.println("--- Shutdown Tests ---");
		*conMan.setLocalAddress("999");
		*conMan.destroyConnectionEndpoint("Alice");
		*System.out.println("Checking all existing connectionEndpoints: " + conMan.returnAllConnections());
		*System.out.println("State of Alice: " + conMan.getConnectionState("Alice"));
		*System.out.println("State of Bob: " + conMan.getConnectionState("Bob"));
		*System.out.println("--- End of Shutdown Tests ---");
		**/
	}
	
}
