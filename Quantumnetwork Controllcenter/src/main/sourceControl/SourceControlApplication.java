package sourceControl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import communicationList.CommunicationList;
import communicationList.SQLiteCommunicationList;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.PortIsInUseException;
import frame.Configuration;
import graphicalUserInterface.SettingsDialog;
import messengerSystem.SignatureAuthentication;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionManager;
import networkConnection.NetworkPackage;


/**This is the Photon Source API
 * When launched, it creates a server that accepts incoming connections and thereby allows the main Application to signal the Photon Source when to begin sending Photons
 * and where.
 * It should be launched with 2 parameters: the IP and the Port. Example: "SCAPI.jar 127.0.0.1 4422" would open a local server on port 4422.
 * After a signal has been received, it closes the connection again.
 * 
 * In practice, a received signal will result in a .txt file being written in the local ReceivedSignals-Folder, that contains all the relevant information that was transmitted alongside the signal itself.
 * 
 * @author Jonas Huehne
 *
 */

public class SourceControlApplication {

	private static String ip;
	private static int port;
	public static ConnectionManager conMan;
	public static CommunicationList communicationList;
	public static SignatureAuthentication authentication;
	
	/**
	 * Launches the Source Control Application
	 * @param args
	 * 		args[0] IP of this machine, which other members of the network will connect to <br>
	 * 		args[1] local server port
	 */
	public static void main(String[] args) {
		
		// Configuration Init
		try {
			Configuration.findProperties();
			Configuration.createFolders();
					
			//Init ApplicationSettings
			SettingsDialog.initSettings();
		} catch (IOException e) {
			System.err.println("ERROR: Configuration failed: " + e);
		}
		
		ip = args[0];
		port = Integer.valueOf(args[1]);
		System.out.println("Starting the Source Control on IP: " + ip + " and Port: " + String.valueOf(port) + "!");

		/*
		 *TODO: Check if Sig files exist and if not, generate them!
		 */
		
		try {
			conMan = new ConnectionManager(ip,port);
		} catch (IOException e) {
			System.err.println("A " + e.getClass().getSimpleName() + " occurred trying to create the ConnectionManager for the Photon Source. Shutting down.");
			e.printStackTrace();
			return;
		} catch (PortIsInUseException e) {
			System.err.println("A " + e.getClass().getSimpleName() + " occurred trying to create the ConnectionManager for the Photon Source. Shutting down.");
			e.printStackTrace();
			return;
		}
		
		// Communication List Init
		communicationList = new SQLiteCommunicationList();

		// Authentication Init
		authentication = new SHA256withRSAAuthentication();
		
		
	}
	
	public static void writeSignalFile(NetworkPackage transmission, String senderID) {

		String fileName = transmission.getTypeArg();
		String sourceInfo = MessageSystem.byteArrayToString(transmission.getContent());
		Writer inWriter;
		Path inFilePath = Path.of(System.getProperty("user.dir") + File.separator + "Signals" + File.separator + fileName + ".txt");
		try {
			inWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(inFilePath), Configuration.getProperty("Encoding")));
			inWriter.write(sourceInfo);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			conMan.destroyConnectionEndpoint(senderID);
		} catch (ManagerHasNoSuchEndpointException e) {
			e.printStackTrace();
		}

	}

}
