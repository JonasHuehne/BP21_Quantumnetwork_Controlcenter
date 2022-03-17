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
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.SettingsDialog;
import messengerSystem.SignatureAuthentication;
import messengerSystem.MessageSystem;
import messengerSystem.SHA256withRSAAuthentication;
import networkConnection.ConnectionManager;
import networkConnection.NetworkPackage;


/**This is the Photon Source API
 * It handles SourceSignals by writing the signals contents to a file and the destroys the connection.
 * 
 * In practice, a received signal will result in a .txt file being written in the local ReceivedSignals-Folder, that contains all the relevant information that was transmitted alongside the signal itself.
 * 
 * @author Jonas Huehne
 *
 */

public class SourceControlApplication {

	public static CommunicationList communicationList;
	public static SignatureAuthentication authentication;

	
	public static void writeSignalFile(NetworkPackage transmission, String senderID) {

		String fileName = transmission.getMessageArgs().fileName();
		String sourceInfo = MessageSystem.byteArrayToString(transmission.getContent());
		Writer inWriter;
		Path basePath = Path.of(System.getProperty("user.dir") + File.separator + "Signals" + File.separator);
		Path inFilePath = Path.of(System.getProperty("user.dir") + File.separator + "Signals" + File.separator + fileName + ".txt");
		try {
			Files.createDirectories(basePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			inWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(inFilePath), Configuration.getProperty("Encoding")));
			inWriter.write(sourceInfo);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		QuantumnetworkControllcenter.conMan.destroySourceConnection(senderID, false);

	}

}
