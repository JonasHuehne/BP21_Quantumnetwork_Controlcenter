package frame;

import networkConnection.ConnectionManager;
import ui.ConsoleUI;
import java.awt.EventQueue;

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
		
		// switched workspace in Eclipse
		
	}
	
}