package frame;

import java.awt.EventQueue;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.swing.UIManager;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import encryptionDecryption.AES256;
import encryptionDecryption.SymmetricCipher;
import exceptions.PortIsInUseException;
import graphicalUserInterface.GUIMainWindow;
import graphicalUserInterface.SettingsDialog;
import keyStore.KeyStoreDbManager;
import messengerSystem.Authentication;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionManager;
import qnccLogger.Log;
import ui.ConsoleUI;

/**
 * Main Class of QuantumnetworkControllcenter
 * 
 * @author Lukas Dentler, Sasha Petri, Jonas Huehne
 */
public class QuantumnetworkControllcenter {
	
	public static ConnectionManager conMan;
	public static CommunicationList communicationList;
	public static Authentication authentication;
	public static GUIMainWindow guiWindow;
	
	static boolean LAUNCH_GUI = true;  // launch GUI
	static boolean LAUNCH_CUI = false; // launch console UI

	public static Log logger;
	
	/**
	 * Method to initialize a Quantumnetwork Controllcenter
	 * @param args <br>
	 * 		args[0] local IP used by the ConnectionManager in this launch, also sets the corresponding entry "UserIP" in the config file
	 * 		args[1] local port used by the ConnectionManager in this launch, also sets the corresponding entry "UserPort" in the config file
	 * 		may be null, in this case the Properties file is not modified
	 * 		args[2] if the 3rd param is "noGUI", the console will be used instead of the GUI.
	 * 		may be null, in this case the Properties file is not modified
	 */
	public static void initialize(String[] args) {
		
		//TODO add initialization of further Classes
		
		//Open GUI or CUI
		if(args != null && args.length == 3 && args[2].equals("noGUI")) {
			LAUNCH_GUI = false;
			LAUNCH_CUI = true;
		}else {
			LAUNCH_GUI = true;
			LAUNCH_CUI = false;
		}

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
		if(args != null && args.length == 2) {
			Configuration.setProperty("UserIP", args[0]);
			Configuration.setProperty("UserPort", args[1]);
		}
		/*
		 *TODO: Check if Sig files exist and if not, generate them!
		 */

		// Communication List Init
		communicationList = new SQLiteCommunicationList();
		
		String userName = Configuration.getProperty("UserName");
		String ip = Configuration.getProperty("UserIP");
		int port = Integer.valueOf(Configuration.getProperty("UserPort"));
		System.out.println("Initialising IP: " + ip + " and Port " + port);
		String localIP = ip;
		int localPort = port;
		try {
			conMan = new ConnectionManager(localIP, localPort, userName, communicationList);
		} catch (IOException | PortIsInUseException e) {
			System.err.println("Could not initialize the ConnectionManager - an  Exception occured. ");
			System.err.println(e.getClass().getCanonicalName() + " - Message: " + e.getMessage());
			e.printStackTrace();
			System.err.println("Shutting down.");
			System.exit(0);
		} 
		MessageSystem.conMan = conMan;

		// Authentication Init
		authentication = new SHA256withRSAAuthentication();
		MessageSystem.setAuthenticationAlgorithm(authentication);

		// Encryption to use
		SymmetricCipher cipher = new AES256();
		MessageSystem.setEncryption(cipher);
		
		try {
			KeyStoreDbManager.createNewKeyStoreAndTable();
		} catch (SQLException e) {
			System.err.println("Could not initialize the Keystore. Shutting down. ");
			e.printStackTrace();
			System.exit(0);
		}
		
		// insert artificial key pair for "KeyTesterA" and "KeyTesterB"
		// so that we can test symmetric encryption
		byte[] keyToInsert = new byte[1024];
		Arrays.fill(keyToInsert, (byte) 101);
		try {
			KeyStoreDbManager.deleteEntryIfExists("KeyTesterA");
			KeyStoreDbManager.deleteEntryIfExists("KeyTesterB");
			KeyStoreDbManager.insertToKeyStore("KeyTesterA", keyToInsert, "", "", false, true);
			KeyStoreDbManager.insertToKeyStore("KeyTesterB", keyToInsert, "", "", false, true);
		} catch (SQLException e) {
			System.err.println("Could not initialize the entries of the keystore. Shutting down. ");
			e.printStackTrace();
			System.exit(0);
		}
		
		
		System.out.println("QuantumnetworkControllcenter initialized");
	}
	 
	
	/**
	 * Main-method to run QuantumnetworkControllcenter
	 * @param args <br>
	 * 		args[0] local IP used by the ConnectionManager in this launch, also sets the corresponding entry "UserIP" in the config file
	 * 		args[1] local port used by the ConnectionManager in this launch, also sets the corresponding entry "UserPort" in the config file
	 * 		may be null, in this case the Properties file is not modified
	 */
	public static void main(String[] args) {
		

		logger = new Log("QNCC Logger");
		logger.loggerShowInfos();
		logger.logInfo("Run QuantumnetworkControllcenter initialization.");
		
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
					if (LAUNCH_CUI) {
						ConsoleUI consoleWindow = new ConsoleUI();
					}
					if (LAUNCH_GUI) {
						guiWindow = new GUIMainWindow();
						guiWindow.getFrame().setVisible(true);
					}
				} catch (Exception e) {
					System.err.println("Something went wrong trying to launch the GUI or Console UI.");
					e.printStackTrace();
				}
			}
		});
		
	}


}
