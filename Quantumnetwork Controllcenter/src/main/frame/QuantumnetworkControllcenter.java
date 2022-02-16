package frame;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import graphicalUserInterface.GUIMainWindow;
import messengerSystem.Authentication;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionManager;
import ui.ConsoleUI;
import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.UIManager;

import messengerSystem.MessageSystem;

/**
 * Main Class of QuantumnetworkControllcenter
 * 
 * @author Lukas Dentler
 */
public class QuantumnetworkControllcenter {
	
	public static ConnectionManager conMan;
	public static CommunicationList communicationList;
	public static Authentication authentication;
	public static GUIMainWindow guiWindow;

	/**
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public static void initialize() {
		
		//TODO add initialization of further Classes

		// Configuration Init
		try {
			Configuration.findProperties();
			Configuration.createFolders();
		} catch (IOException e) {
			System.err.println("ERROR: Configuration failed: " + e);
		}
		
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
		

		
		System.out.println("Run QuantumnetworkControllcenter initialisation");
		
		initialize();
		
		// Look and Feel for Console UI
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println(
					"ERROR - Something went wrong while trying to set the look and feel for the Console UI. "
					+ "The program may appear visually different, however, functionality should not be affected. "
					+ "The following error occured " + System.lineSeparator()
					+ e.getClass().getCanonicalName() + ": " + e.getMessage());
		}
		
		// Launch Console UI
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ConsoleUI consoleWindow = new ConsoleUI();
					guiWindow = new GUIMainWindow();
					guiWindow.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		/*
		communicationList.insert("TestNameAlice", "127.0.0.1", 2300, "testSig01");
		communicationList.insert("TestNameBob", "127.0.0.2", 3300, "testSig02");
		communicationList.insert("TestNameCharlie", "127.0.0.3", 4300, "testSig03");
		*/

		
	}


}
