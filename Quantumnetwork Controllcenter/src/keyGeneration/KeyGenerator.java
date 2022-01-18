package keyGeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private Path connectionPath;
	private Path localPath;
	private Thread transferThread;
	private String expectedOutgoingFilename = "out.txt";
	private String expectedIncomingFilename = "in.txt";
	
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
		
		System.out.println("Starting KeyGen MessaginService");
		KeyGenMessagingService();
		
	}

	
	/**Returns true only if all checks are completed successfully.
	 * 
	 * @return Can we start the KeyGeneration?
	 */
	private boolean preGenChecks(String connectionID) {
		boolean check = true;
		
		//Check if active connection is selected
		check = check && MessageSystem.getActiveConnection().equals(connectionID);
		
		//Check if active connection is connected
		check = check && MessageSystem.conMan.getConnectionState(MessageSystem.getActiveConnection()).equals(ConnectionState.CONNECTED);
		
		
		
		return check;
		
	}
	
	
	/**Returns true if both ends of the keyGen process agree to begin the generation process.
	 * 
	 * @return
	 */
	private boolean preGenSync() {
		System.out.println("Sending Sync Request via " + MessageSystem.getActiveConnection() + " !");
		//Send Sync Request
		MessageSystem.conMan.getConnectionEndpoint(MessageSystem.getActiveConnection()).pushMessage("sync:::");
		
		
		System.out.println("Starting to wair for response...");
		Instant startWait = Instant.now();
		Instant current;
		//Wait for Answer
		while(!MessageSystem.previewReceivedMessage().equals("syncConfirm") && !MessageSystem.previewReceivedMessage().equals("syncReject")) {
			System.out.println(MessageSystem.previewReceivedMessage());
			current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() >= 10) {
				System.out.println("Time-out while waiting for Pre-Key-Generation Sync. Did not recieve an Accept- or Reject-Answer in time");
				return false;
			}
		}
		String msg = MessageSystem.readReceivedMessage();
		System.out.println("Recieved Sync-Response: " + msg + "!");
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
		System.out.println("[" + connectionID + "]: Add confirmation-promt for KeyGen here!");
		//for now, always accept
		boolean accept = true;
		if(accept) {
			setUpFolders();
			MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage("msg:::syncConfirm");
			transferData();
		}else {
			MessageSystem.conMan.getConnectionEndpoint(connectionID).pushMessage("msg:::syncReject");
		}
		
	}
	
	private void KeyGenMessagingService() {
		if(!setUpFolders()) {
			System.out.println("Aborting KeyGenMessagingService, some Folders could not be found!");
			return;
		}
		transferData();
		
	}
	
	private void transferData() {
		transferThread = new Thread(this, connectionID + "_transferThread");
		transferThread.start();
	}
	
	/**Checks and prepares all necessary folders.
	 * 
	 * @return Returns true if it found the python Script folder and created or found a folder with the name of the connectionEndpoint that owns this KeyGenerator.
	 */
	private boolean setUpFolders() {
		
		//Get own root folder
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
        System.out.println(currentWorkingDir.normalize().toString());
        localPath = currentWorkingDir;
        
        //Get python folder
        Path pythonScriptLocation = currentWorkingDir.resolve("python");
        System.out.println(pythonScriptLocation.normalize().toString());
        if(!Files.isDirectory(pythonScriptLocation)) {
        	System.out.println("Error, could not find the Python Script folder, expected: " + pythonScriptLocation.normalize().toString());
        	return false;
        }
        pythonPath = pythonScriptLocation;
        
        //Prepare Connection Folder
        Path connectionFolderLocation = currentWorkingDir.resolve("connections");
        connectionFolderLocation = connectionFolderLocation.resolve(connectionID);
        System.out.println(connectionFolderLocation.normalize().toString());
        if(!Files.isDirectory(connectionFolderLocation)) {
        	System.out.println("Could not find the Connection folder for "+ connectionID +", expected: " + connectionFolderLocation.normalize().toString() + " Creating folder now!");
        	try {
				Files.createDirectories(connectionFolderLocation);
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
	
	
	@Override
	public void run() {
		while(true) {
			Path outFilePath = connectionPath.resolve(expectedOutgoingFilename);
			Path inFilePath = connectionPath.resolve(expectedIncomingFilename);
			
			//Read outgoing file and send it.
			if(Files.exists(outFilePath)) {

				//Read FileContent
				Stream<String> outFileReader;
				String outFileContent = "";
				try {
					outFileReader = Files.lines(outFilePath);
					outFileContent = outFileReader.collect(Collectors.joining("\n"));
				    outFileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			    //Send FileContent
				synchronized(this){
			    String prevActiveConn = MessageSystem.getActiveConnection();
			    MessageSystem.setActiveConnection(connectionID);
			    MessageSystem.sendMessage(outFileContent);
			    System.out.println("123");
			    MessageSystem.setActiveConnection(prevActiveConn);	    
				}
				
			    //Clear File Content
			    outFileReader = null;
			    outFileContent = "";
			    
			    //Delete out.txt
			    try {
					Files.delete(outFilePath);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				//System.out.println("File not found!");
			}
			
			if(!Files.exists(inFilePath) && QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getMessageStack().size() > 0) {
				String inFileContent = "";
				//Recieve Message
				synchronized(this){
				    String prevActiveConn = MessageSystem.getActiveConnection();
				    MessageSystem.setActiveConnection(connectionID);
				    inFileContent = MessageSystem.readReceivedMessage();
				    MessageSystem.setActiveConnection(prevActiveConn);	    
					}
				
				//System.out.println("[" + connectionID + "]: inFileContent recieved: " + inFileContent);
				
				try		
				(Writer inWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inFilePath.toString()), "utf-8"))) {
					inWriter.write(inFileContent);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}


