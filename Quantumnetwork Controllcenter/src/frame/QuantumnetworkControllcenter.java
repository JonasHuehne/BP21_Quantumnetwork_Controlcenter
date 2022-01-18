package frame;

import networkConnection.ConnectionManager;
import ui.ConsoleUI;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import MessengerSystem.MessageSystem;
import keyGeneration.KeyGenerator;

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
		
		conMan.createNewConnectionEndpoint("Alice", 2300);
		conMan.createNewConnectionEndpoint("Bob", 3300);
		
		conMan.getConnectionEndpoint("Bob").waitForConnection();
		try {
			conMan.getConnectionEndpoint("Alice").establishConnection("127.0.0.1", 3300);
		} catch (IOException e) {
			System.out.println("Error while connection!");
			e.printStackTrace();
		}
		
		/**conMan.getConnectionEndpoint("Alice").pushMessage("msg", "testMessage123");
		MessageSystem.setActiveConnection("Bob");
		
		while(MessageSystem.previewReceivedMessage().equals("")) {
			
		}
		System.out.println("Recieved Message: " + MessageSystem.readReceivedMessage());
		**/
		
		conMan.getConnectionEndpoint("Alice").getKeyGen().generateKey();
		
	}
	
}
