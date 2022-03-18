package sourceControl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import communicationList.CommunicationList;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import messengerSystem.SignatureAuthentication;
import messengerSystem.MessageSystem;
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
		System.out.println("[PhotonSource]: Received Signal, starting to write File!");
		String fileName = transmission.getMessageArgs().fileName();
		String sourceInfo = MessageSystem.byteArrayToString(transmission.getContent());
		Writer inWriter;
		System.out.println("[PhotonSource]: fileName: " + fileName);
		System.out.println("[PhotonSource]: sourceInfo: " + sourceInfo);
		Path basePath = Path.of(System.getProperty("user.dir") + File.separator + "Signals" + File.separator);
		Path inFilePath = Path.of(System.getProperty("user.dir") + File.separator + "Signals" + File.separator + fileName + ".txt");
		try {
			System.out.println("[PhotonSource]: Creating Folders at: " + basePath);
			Files.createDirectories(basePath);
		} catch (IOException e1) {
			System.out.println("[PhotonSource]: Folder-Creation failed!");
			e1.printStackTrace();
		}
		try {
			inWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(inFilePath), Configuration.getProperty("Encoding")));
			System.out.println("Writing content: " + sourceInfo + " in file: " + inFilePath + "!");
			inWriter.write(sourceInfo);
			inWriter.close();
			System.out.println("[PhotonSource]: Writing File!");
		} catch (IOException e) {
			System.out.println("[PhotonSource]: Error while writing File!");
			e.printStackTrace();
		}
		System.out.println("[PhotonSource]: ---Completed Signal-processing, sending destroy order via CE!---");
		QuantumnetworkControllcenter.conMan.destroySourceConnection(senderID, false);

	}

}
