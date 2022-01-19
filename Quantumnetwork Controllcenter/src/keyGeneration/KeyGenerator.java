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
import MessengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionState;

/**This class contains everything that it needed to generate a secure key.
 * After the key is generated, it is stored in the KeyDB.
 * 
 * @author Jonas Huehne
 *
 */

public class KeyGenerator implements Runnable{
	
	private String connectionID;
	private Path pythonPath;
	private String pythonScriptName = "examplePythonScript.py";
	private int initiative = 0;
	private Path connectionPath;
	private Path localPath;
	private Thread transferThread;
	private String expectedOutgoingFilename = "out.txt";
	private String expectedIncomingFilename = "in.txt";
	private String expectedKeyFilename = "key.txt";
	private String expectedTermination = "terminate.txt";
	private boolean keyGenRunning;
	
	public KeyGenerator(String connectionID) {
		this.connectionID = connectionID;
	}
	
	
	/**Generate a Key by using the python scrips and acting as a middelman between both involved parties,
	 *  by handling the network side of the key generation as well as storing the key in the KeyDB.
	 * 
	 */
	public void generateKey() {
		System.out.println("Starting KeyGenProcess!");
		//Check if everything is ready
		System.out.println("Performing preGenChecks!");
		if(!preGenChecks(connectionID)) {
			System.out.println("Generation of Key did not start, preGenChecks failed!");
			return;
		}
		System.out.println("Performing preGenSync!");
		//Wait for syncConfirm message before continuing.
		if(!preGenSync()) {
			System.out.println("preGenSync failed!");
			return;
		}
		System.out.println("preGenSync successful");
		initiative = 1;
		
		System.out.println("Starting KeyGen MessagingService");
		
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
			System.out.println("Calling the python script with the following line: " + "python " + pythonPath.resolve(pythonScriptName) + " " + initiative + " " + connectionPath);
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
		System.out.println("Sending Sync Request via " + connectionID + " !");
		//Send Sync Request
		MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage("sync", "");
		
		
		System.out.println("Starting to wait for response...");
		Instant startWait = Instant.now();
		Instant current;
		//Wait for Answer
		while((MessageSystem.previewReceivedMessage(connectionID) == null) || ((!MessageSystem.previewReceivedMessage(connectionID).getHead().equals("syncConfirm")) && (!MessageSystem.previewReceivedMessage(connectionID).getHead().equals("syncReject")))) {
			if(MessageSystem.previewReceivedMessage(connectionID) != null) {
				//System.out.println(MessageSystem.previewReceivedMessage().getContent());
			}
			current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() >= 10) {
				System.out.println("Time-out while waiting for Pre-Key-Generation Sync. Did not recieve an Accept- or Reject-Answer in time");
				return false;
			}
		}
		String msg = MessageSystem.readReceivedMessage(connectionID);
		System.out.println("Received Sync-Response: " + msg + "!");
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
			MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage("syncConfirm", "syncConfirm");
			initiative = 0;
			keyGenMessagingService();
		}else {
			MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage("syncReject", "syncReject");
		}
		
	}
	
	/**This calls the python script and also starts the threads that handle the .txt files.
	 * 
	 */
	private void keyGenMessagingService() {
		if(!setUpFolders()) {
			System.out.println("Aborting KeyGenMessagingService, some Folders could not be found!");
			return;
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
        	System.out.println("Error, could not find the Python Script folder, expected: " + pythonScriptLocation.normalize().toString());
        	return false;
        }
        pythonPath = pythonScriptLocation;
        
        //Prepare Connection Folder
        Path connectionFolderLocation = currentWorkingDir.resolve("connections");
        connectionFolderLocation = connectionFolderLocation.resolve(connectionID);
        //System.out.println(connectionFolderLocation.normalize().toString());
        if(!Files.isDirectory(connectionFolderLocation)) {
        	System.out.println("Could not find the Connection folder for "+ connectionID +", expected: " + connectionFolderLocation.normalize().toString() + " Creating folder now!");
        	try {
				Files.createDirectories(connectionFolderLocation);
			} catch (IOException e) {
				System.out.println("Error: Could not create Connection Folder!");
				e.printStackTrace();
				return false;
			}
        }else {
        	System.out.println("Connection Folder found.");
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
				System.out.println("Error while reading the Key File!");
				e.printStackTrace();
			}
		}else {
			System.out.println("Error while trying to read the Key File! Either no " + expectedKeyFilename + " file was found or there was still a .lock file present!");
		}
		
		//Insert into DB
		String ownAddress = MessageSystem.conMan.getConnectionEndpoint(connectionID).getLocalAddress();
		int ownPort = MessageSystem.conMan.getConnectionEndpoint(connectionID).getServerPort();
		
		String remoteAddress = MessageSystem.conMan.getConnectionEndpoint(connectionID).getRemoteAddress();
		int remotePort = MessageSystem.conMan.getConnectionEndpoint(connectionID).getRemotePort();
		
		//TODO: Check in with Aron to agree on interface according to standards. Ideally change DB Method Parameter to Byte[].
		//KeyStoreDbManager.insertToDb(connectionID, key, 0, ownAddress + ":" + String.valueOf(ownPort), remoteAddress + ":" + String.valueOf(remotePort));
		
		//End the KeyGen Process and clean up.
		shutdownKeyGen(false);
	}
	
	/**This handles the shutdown of the KeyGen. It is called locally and removes all files that may be left behind.
	 * It is called if the process is aborted/terminated or completed. If relay == true, this also calls the other involved party.
	 * 
	 * @param relay if True, this will cause a network Message to be sent.
	 */
	public void shutdownKeyGen(boolean relay) {
		
		System.out.println("Shutting down the KeyGen of " + connectionID);
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
				MessageSystem.sendSignal(connectionID, "terminate");
			}
			
		} catch (IOException e) {
			System.out.println("Error while shutting down the KeyGen and deleting any potentially existing Files!");
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
					MessageSystem.sendMessage(connectionID, new String(outFileContent, "ISO-8859-1"));
				} catch (UnsupportedEncodingException e) {
					System.out.println("Error: unsupportet Encoding: ISO-8859-1!");
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
					System.out.println("Adding Key to KeyDB");
					transferKeyFileToDB();
					return;
				}
				
				
				//Abort Signal File
				if(Files.exists(connectionPath.resolve(expectedTermination)) && Files.notExists(connectionPath.resolve(expectedTermination + ".lock"))) {
					System.out.println("Aborting Key Generation");
				
					try {
						Files.delete(connectionPath.resolve(expectedTermination));
						shutdownKeyGen(true);
					} catch (IOException e) {
						shutdownKeyGen(true);
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
				inFileContent = MessageSystem.readReceivedMessage(connectionID);    
				
				//Write temporary lock file
				File lockFile = new File(connectionPath.resolve(inFilePath + ".lock").toString());
				try {
					lockFile.createNewFile();
				} catch (IOException e1) {
					System.out.println("Error while creating temp lock file for the new in.txt");
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


