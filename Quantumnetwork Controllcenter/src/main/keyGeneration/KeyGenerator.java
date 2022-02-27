package keyGeneration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import messengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import keyStore.KeyStoreDbManager;
import networkConnection.ConnectionState;
import networkConnection.TransmissionTypeEnum;


/**This class contains everything that it needed to generate a secure key.
 * After the key is generated, it is stored in the KeyDB.
 * 
 * @author Jonas Huehne
 *
 */

public class KeyGenerator implements Runnable{
	
	private String connectionID;
	private Path pythonPath;
	private String pythonScriptName = "examplePythonScript.py";	//Use this to set the name of the python script used to generate a key.
	private int initiative = 0;	//This is used to determine which side of a connection should execute which side of the KeyGenProcess
	private Path connectionPath;
	private Path localPath;
	private Thread transferThread;
	private String expectedOutgoingFilename = "out.txt";	//Use this to set the filename of the files that the python script wants to send to the other side of the connection.
	private String expectedIncomingFilename = "in.txt";	//Use this to set the filename of the files that the local python script should read from and then delete.
	private String expectedKeyFilename = "key.txt";	//Use this to set the filename of the key file that is generated by the python script and then read by this program in order to store the key in the KeyDB.
	private String expectedTermination = "terminate.txt";	//Use this to set the filename of the terminate signal wirtten by the local python script to signal this program to stop the KeyGen Process.
	private String expectedPythonTerm = "pythonTerm.txt";	//Use this to set the filename of the signal for the python script to terminate the KeyGen Process. This is created if the program was told to shutdown from the other side of the connection.
	private boolean keyGenRunning;
	
	/**A new KeyGenerator is added for each ConnectionEndpoint automatically and is supplied that CEs ID.
	 * 
	 * @param connectionID the name of the owning ConnectionEndpoint.
	 */
	public KeyGenerator(String connectionID) {
		this.connectionID = connectionID;
	}
	
	
	/**Generate a Key by using the python scrips and acting as a middelman between both involved parties,
	 *  by handling the network side of the key generation as well as storing the key in the KeyDB.
	 * 
	 */
	public void generateKey() {
		System.out.println("[" + connectionID + "]: Starting KeyGenProcess!");
		//Check if everything is ready
		System.out.println("[" + connectionID + "]: Performing preGenChecks!");
		if(!preGenChecks(connectionID)) {
			System.err.println("[" + connectionID + "]: Generation of Key did not start, preGenChecks failed!");
			return;
		}
		
		System.out.println("[" + connectionID + "]: Performing preGenSync!");
		//Wait for syncConfirm message before continuing.
		if(!preGenSync()) {
			System.err.println("[" + connectionID + "]: preGenSync failed!");
			return;
		}
		System.out.println("[" + connectionID + "]: preGenSync successful");
		initiative = 1;
		
		System.out.println("[" + connectionID + "]: Starting KeyGen MessagingService");
		
		//Signal the Source
		signalSourceAPI();
		
		//Start the process
		keyGenMessagingService();
		
	}
	
	/**This calls the python script that was set at the top of this class as pythonScriptName.
	 * The script is expected to be in localFolder/python/
	 */
	private void signalPython() {
		try {
			System.out.println("[" + connectionID + "]: Calling the python script with the following line: " + "python " + pythonPath.resolve(pythonScriptName) + " " + initiative + " " + connectionPath);
			Runtime.getRuntime().exec("python " + pythonPath.resolve(pythonScriptName) + " " + initiative + " " + connectionPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Dummy Method for signaling the source API.
	 * Needs to be added once the API exists.
	 */
	private void signalSourceAPI() {
		//TODO: add Implementation depending on SourceAPI.
		return;
	}

	
	/**Returns true only if all checks are completed successfully.
	 * 
	 * @return Can we start the KeyGeneration?
	 */
	private boolean preGenChecks(String connectionID) {
		boolean check = true;
		
		//Check if active connection is connected
		check = check && MessageSystem.conMan.getConnectionState(connectionID).equals(ConnectionState.CONNECTED);
		
		check = check && MessageSystem.getNumberOfPendingMessages(connectionID) == 0;
		
		return check;
		
	}
	
	
	/**Returns true if both ends of the keyGen process agree to begin the generation process.
	 * 
	 * @return true means the other party agreed and is checked and ready.
	 */
	private boolean preGenSync() {
		System.out.println("[" + connectionID + "]: Sending Sync Request via " + connectionID + " !");
		//Send Sync Request
		//MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage(TransmissionTypeEnum.KEYGEN_SYNC_REQUEST, "", "");
		MessageSystem.sendAuthenticatedMessage(connectionID, TransmissionTypeEnum.KEYGEN_SYNC_REQUEST, "","");
		
		
		System.out.println("[" + connectionID + "]: Starting to wait for response...");
		Instant startWait = Instant.now();
		Instant current;
		//Wait for Answer
		while(true) {
			//Wait for authenticated Transmission of KeyGenResponse(message and signature == 2)
			if(MessageSystem.getNumberOfPendingMessages(connectionID) > 0) {
				if(MessageSystem.previewReceivedMessage(connectionID).getHead() == TransmissionTypeEnum.KEYGEN_SYNC_ACCEPT || MessageSystem.previewReceivedMessage(connectionID).getHead() == TransmissionTypeEnum.KEYGEN_SYNC_REJECT) {
					break;
				}
			}
			
			
			current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() >= 10) {
				System.err.println("[" + connectionID + "]: Time-out while waiting for Pre-Key-Generation Sync. Did not recieve an Accept- or Reject-Answer in time");
				return false;
			}
		}
		String msg = MessageSystem.readReceivedMessage(connectionID).getContent();
		System.out.println("[" + connectionID + "]: Received Sync-Response: " + msg + "!");
		if(msg.equals("syncConfirm")) {
			return true;
		}
		
		System.out.println("SyncRequest Rejected!");
		return false;
	}
	
	
	/**This is called if a message asking for preGenSync was received. It will ask the user if the keyGenProcess should be started and sends the appropriated message back.
	 * 
	 */
	public void keyGenSyncResponse() {
		System.out.println("[" + connectionID + "]: Add confirmation-prompt for KeyGen here!");
		//for now, always accept
		boolean accept = true;
		if(accept && preGenChecks(connectionID)) {
			signalSourceAPI();
			MessageSystem.sendAuthenticatedMessage(connectionID, TransmissionTypeEnum.KEYGEN_SYNC_ACCEPT, "", "syncConfirm");
			initiative = 0;
			keyGenMessagingService();
		}else {
			MessageSystem.sendAuthenticatedMessage(connectionID, TransmissionTypeEnum.KEYGEN_SYNC_REJECT, "", "syncReject");
		}
		
	}
	
	/**This calls the python script and also starts the threads that handle the .txt files.
	 * 
	 */
	private void keyGenMessagingService() {
		if(!setUpFolders()) {
			System.err.println("[" + connectionID + "]: Aborting KeyGenMessagingService, some Folders could not be found!");
			return;
		}
		
		//Remove potential remnant from a previous, terminated attempt
		if(Files.exists(connectionPath.resolve(expectedPythonTerm))){
			try {
				Files.deleteIfExists(connectionPath.resolve(expectedPythonTerm));
			} catch (IOException e) {
				System.err.println("[" + connectionID + "]: Failed while cleaning up pythonterm file from previous generation process. Attempted to delete file " + connectionPath.resolve(expectedPythonTerm).toString());
				e.printStackTrace();
				return;
			}
		}
		
		//calling python
		signalPython();
		
		
		transferData();
		
	}
	
	
	/**This starts the actual thread that deals with the message handling.
	 * 
	 */
	private void transferData() {
		if(keyGenRunning) {
			System.out.println("Error: Key Gen Thread was already running, could not start a second one!");
		}
		transferThread = new Thread(this, connectionID + "_transferThread");
		keyGenRunning = true;
		transferThread.start();
	}
	
	/**Checks and prepares all necessary folders.
	 * 
	 * @return Returns true if it found the python Script folder and created or found a folder with the name of the connectionEndpoint that owns this KeyGenerator.
	 */
	private boolean setUpFolders() {
		
		//Get own root folder
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
        //System.out.println(currentWorkingDir.normalize().toString());
        localPath = currentWorkingDir;
        
        //Get python folder
        Path pythonScriptLocation = localPath.resolve("python");
        //System.out.println(pythonScriptLocation.normalize().toString());
        if(!Files.isDirectory(pythonScriptLocation)) {
        	System.err.println("[" + connectionID + "]: Error, could not find the Python Script folder, expected: " + pythonScriptLocation.normalize().toString());
        	return false;
        }
        pythonPath = pythonScriptLocation;
        
        //Prepare Connection Folder
        Path connectionFolderLocation = currentWorkingDir.resolve("connections");
        connectionFolderLocation = connectionFolderLocation.resolve(connectionID);
        //System.out.println(connectionFolderLocation.normalize().toString());
        if(!Files.isDirectory(connectionFolderLocation)) {
        	System.out.println("[" + connectionID + "]: Could not find the Connection folder for "+ connectionID +", expected: " + connectionFolderLocation.normalize().toString() + " Creating folder now!");
        	try {
				Files.createDirectories(connectionFolderLocation);
			} catch (IOException e) {
				System.err.println("[" + connectionID + "]: Error: Could not create Connection Folder!");
				e.printStackTrace();
				return false;
			}
        }else {
        	System.out.println("[" + connectionID + "]: Connection Folder found.");
        }
        connectionPath = connectionFolderLocation;
        
		return true;
	}
	
	
	/**Transfers the contents of a key.txt file to the DB.
	 * Needs to be adjusted if the DB is not changed to use Byte[].
	 */
	private void transferKeyFileToDB() {
		//Read Key from File
		byte[] key = null;
		Path keyFilePath = connectionPath.resolve(expectedKeyFilename);
		if(Files.exists(keyFilePath) && Files.notExists(connectionPath.resolve(expectedKeyFilename + ".lock"))) {
			try {
				//Read
				key = Files.readAllBytes(keyFilePath);
				
				//Delete
				try {
					Files.delete(keyFilePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				System.err.println("[" + connectionID + "]: Error while reading the Key File!");
				e.printStackTrace();
			}
		}else {
			System.err.println("[" + connectionID + "]: Error while trying to read the Key File! Either no " + expectedKeyFilename + " file was found or there was still a .lock file present!");
		}
		
		//Insert into DB
		String ownAddress = MessageSystem.conMan.getConnectionEndpoint(connectionID).getLocalAddress();
		int ownPort = MessageSystem.conMan.getConnectionEndpoint(connectionID).getServerPort();
		
		String remoteAddress = MessageSystem.conMan.getConnectionEndpoint(connectionID).getRemoteAddress();
		int remotePort = MessageSystem.conMan.getConnectionEndpoint(connectionID).getRemotePort();
		
		//Store the new Key in the KeyDB
		KeyStoreDbManager.createNewKeyStoreAndTable();
		//Overwrite if Key already exists for connectionID
		if(KeyStoreDbManager.doesKeyStreamIdExist(connectionID)) {
			KeyStoreDbManager.deleteKeyInformationByID(connectionID);
		}
		KeyStoreDbManager.insertToKeyStore(connectionID, key, 0, ownAddress + ":" + String.valueOf(ownPort), remoteAddress + ":" + String.valueOf(remotePort), false);
		
		//End the KeyGen Process and clean up.
		shutdownKeyGen(false, false);
	}
	
	/**This handles the shutdown of the KeyGen. It is called locally and removes all files that may be left behind.
	 * It is called if the process is aborted/terminated or completed. If relay == true, this also calls the other involved party.
	 * 
	 * @param relay if True, this will cause a network Message to be sent.
	 * @param informPython if True, the shutdown originates from inside this program and not from a key- or shudownfile. As such, the pythonScript needs to me notified about this via an expectedPythonTerm-file.
	 */
	public void shutdownKeyGen(boolean relay, boolean informPython) {
		
		System.out.println("[" + connectionID + "]: Shutting down the KeyGen of " + connectionID);
		keyGenRunning = false;
		try {
			Files.deleteIfExists(connectionPath.resolve(expectedOutgoingFilename));
			Files.deleteIfExists(connectionPath.resolve(expectedOutgoingFilename + ".lock"));
			Files.deleteIfExists(connectionPath.resolve(expectedIncomingFilename));
			Files.deleteIfExists(connectionPath.resolve(expectedIncomingFilename + ".lock"));
			Files.deleteIfExists(connectionPath.resolve(expectedKeyFilename));
			Files.deleteIfExists(connectionPath.resolve(expectedKeyFilename + ".lock"));
			Files.deleteIfExists(connectionPath.resolve(expectedTermination));
			Files.deleteIfExists(connectionPath.resolve(expectedTermination + ".lock"));
			
			if(relay) {
				MessageSystem.sendSignal(connectionID, TransmissionTypeEnum.KEYGEN_TERMINATION, "");
			}
			if(informPython) {
				//Signal the local python script that the other end of the connection has terminated the KeyGen Process
				Writer pythonTermWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(connectionPath.resolve(expectedPythonTerm).toString()), "ISO-8859-1"));
				pythonTermWriter.write("");
			}
			
		} catch (IOException e) {
			System.err.println("[" + connectionID + "]: Error while shutting down the KeyGen and deleting any potentially existing Files!");
			e.printStackTrace();
		}
	}
	
	/**This is the tread that runs in the background and listens for .txt files, reads/writes them and then sends and deletes the files where needed.
	 * 
	 */
	@Override
	public void run() {
		
		Path outFilePath = connectionPath.resolve(expectedOutgoingFilename);
		Path inFilePath = connectionPath.resolve(expectedIncomingFilename);
		
		while(keyGenRunning) {	
			
			//Part 1
			//Read outgoing file and send it.
			if(Files.exists(outFilePath) && Files.notExists(connectionPath.resolve(expectedOutgoingFilename + ".lock"))) {

				//Read FileContent
				byte[] outFileContent = null;
				try {
					outFileContent = Files.readAllBytes(outFilePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			    //Send FileContent
			    try {
					MessageSystem.sendAuthenticatedMessage(connectionID, new String(outFileContent, "ISO-8859-1"));
				} catch (UnsupportedEncodingException e) {
					System.err.println("[" + connectionID + "]: Error: unsupportet Encoding: ISO-8859-1!");
					e.printStackTrace();	    
				}
				
			    //Clear File Content
			    outFileContent = null;
			    
			    //Delete out.txt
			    try {
					Files.delete(outFilePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else {
				//Check for non-transmission Files
				
				//Key Result
				if(Files.exists(connectionPath.resolve(expectedKeyFilename)) && Files.notExists(connectionPath.resolve(expectedKeyFilename + ".lock"))) {
					System.out.println("[" + connectionID + "]: Adding Key to KeyDB");
					transferKeyFileToDB();
					return;
				}
				
				
				//Abort Signal File
				if(Files.exists(connectionPath.resolve(expectedTermination)) && Files.notExists(connectionPath.resolve(expectedTermination + ".lock"))) {
					System.out.println("[" + connectionID + "]: Aborting Key Generation");
				
					try {
						Files.delete(connectionPath.resolve(expectedTermination));
						shutdownKeyGen(true, false);
					} catch (IOException e) {
						shutdownKeyGen(true, false);
						e.printStackTrace();
					}
					return;
				}
				
				
				
			}
			//Part 2
			//Writing Incoming Files
			if(!Files.exists(inFilePath) && QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getMessageStack().size() > 0) {
				String inFileContent = null;
				
				//Receive Message
				inFileContent = MessageSystem.readReceivedMessage(connectionID).getContent();    
				
				//Write temporary lock file
				File lockFile = new File(connectionPath.resolve(inFilePath + ".lock").toString());
				try {
					lockFile.createNewFile();
				} catch (IOException e1) {
					System.err.println("[" + connectionID + "]: Error while creating temp lock file for the new in.txt");
					e1.printStackTrace();
				}
				//Write to file
				try		
				(Writer inWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inFilePath.toString()), "ISO-8859-1"))) {
					inWriter.write(inFileContent);
				} catch (UnsupportedEncodingException e) {

					e.printStackTrace();
				} catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
				//Remove .lockFile
				lockFile.delete();
			}
		}
	}
}


