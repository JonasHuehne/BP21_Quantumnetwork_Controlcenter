package frame;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import graphicalUserInterface.GUIMainWindow;
import graphicalUserInterface.SettingsDialog;
import messengerSystem.Authentication;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.TransmissionTypeEnum;
import ui.ConsoleUI;
import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.UIManager;
import java.util.concurrent.TimeUnit;

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
	
	/*
	 * This could be done via args as well.
	 */
	static final boolean launchGUI = true;  // launch GUI
	static final boolean launchCUI = false; // launch console UI

	/**
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public static void initialize(String[] args) {
		
		//TODO add initialization of further Classes

		// Configuration Init
		try {
			Configuration.findProperties();
			Configuration.createFolders();
			
			//Init ApplicationSettings
			SettingsDialog.initSettings();
		} catch (IOException e) {
			System.err.println("ERROR: Configuration failed: " + e);
		}
		
		//Network Connection Init
		if(args.length == 2) {
			Configuration.setProperty("UserIP", args[0]);
			Configuration.setProperty("UserPort", args[1]);
		}
		
		
		String IP = Configuration.getProperty("UserIP");
		int Port = Integer.valueOf(Configuration.getProperty("UserPort"));
		System.out.println("Initialising IP: " + IP + " and Port " + Port);
		String localIP = IP;
		int localPort = Port;
		conMan = new ConnectionManager(localIP, localPort);
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
		
		initialize(args);
		
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
					if (launchCUI) {
						ConsoleUI consoleWindow = new ConsoleUI();
					}
					if (launchGUI) {
						guiWindow = new GUIMainWindow();
						guiWindow.getFrame().setVisible(true);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}


}
