package main.frame;

import main.communicationList.CommunicationList;
import main.communicationList.SQLiteCommunicationList;
import main.keyStore.KeyStoreDbManager;
import main.messengerSystem.Authentication;
import main.messengerSystem.SHA256withRSAAuthentication;
import main.networkConnection.ConnectionManager;
import main.networkConnection.TransmissionTypeEnum;
import main.ui.ConsoleUI;
import java.awt.EventQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import main.messengerSystem.MessageSystem;

/**
 * Main Class of QuantumnetworkControllcenter
 * 
 * @author Lukas Dentler
 */
public class QuantumnetworkControllcenter {
	
	public static ConnectionManager conMan;
	public static CommunicationList communicationList;
	public static Authentication authentication;

	/**
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public static void initialize() {
		
		//TODO add initialization of further Classes
		
		//Network Connection Init
		String localIP = "127.0.0.1";//Must be changed manually as of right now. Use IP depending on intended communication Range (local Machine, local Network or Internet)
		conMan = new ConnectionManager(localIP);
		MessageSystem.conMan = conMan;

		// Communication List Init
		communicationList = new SQLiteCommunicationList();

		// Authentication Init
		authentication = new SHA256withRSAAuthentication();
		
		System.out.println("QuantumnetworkControllcenter initialized");
	}
	 
	
	/**
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
	}
	
}
