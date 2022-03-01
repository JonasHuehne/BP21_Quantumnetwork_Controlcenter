package frame;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import graphicalUserInterface.GUIMainWindow;
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

	/**
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public static void initialize(String IP, int Port) {
		
		//TODO add initialization of further Classes

		// Configuration Init
		try {
			Configuration.findProperties();
			Configuration.createFolders();
		} catch (IOException e) {
			System.err.println("ERROR: Configuration failed: " + e);
		}
		
		//Network Connection Init
		System.out.println("Initialising IP: " + IP + " and Port " + Port);
		String localIP = IP;//"127.0.0.1";//Will be part of the Settings/Properties
		int localPort = Port;//5000;//Will be part of the Settings/Properties
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
		
		initialize(args[0], Integer.valueOf(args[1]));
		
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
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("--------------------------------------------------------------------------------------------");
		System.out.println("---Creating new connection Endpoint. It will try to connect to a target Server directly.---");
		ConnectionEndpoint ce1 = conMan.createNewConnectionEndpoint("ce1", "192.168.0.52", 4000);

		try {
			TimeUnit.SECONDS.sleep(4);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ce1.pushMessage(TransmissionTypeEnum.TRANSMISSION, "", MessageSystem.stringToByteArray("123 Testnachricht über neue Verbindung"), "");
		
		
		try {
			TimeUnit.SECONDS.sleep(8);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("--------------------------------------------------------------------------------------------");
		System.out.println("---Creating 2nd new connection Endpoint. It will try to connect to a target Server directly.---");
		ConnectionEndpoint ce2 = conMan.createNewConnectionEndpoint("ce2", "192.168.0.52", 4000);
		
		try {
			TimeUnit.SECONDS.sleep(4);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ce1.pushMessage(TransmissionTypeEnum.TRANSMISSION, "", MessageSystem.stringToByteArray("ABC Testnachricht über alte Verbindung"), "");
		
		try {
			TimeUnit.SECONDS.sleep(4);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ce2.pushMessage(TransmissionTypeEnum.TRANSMISSION, "", MessageSystem.stringToByteArray("1212 Letzte Testnachricht über das Netzwerk."), "");
		*/
	}


}
