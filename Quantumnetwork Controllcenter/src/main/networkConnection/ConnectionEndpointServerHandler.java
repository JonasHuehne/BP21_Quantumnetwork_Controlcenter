package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import communicationList.CommunicationList;
import communicationList.Contact;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.IpAndPortAlreadyInUseException;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**Every time a connection to the local Server Socket is created, a new instance of ConnectionEndpointServerHandler is also created.
 * The purpose of each CESH is to wait for the first message from the connecting Party, the TransmissionTypeEnum.CONNECTION_REQUEST
 * and use the information contained in it to create a new local ConnectionEndpoint. This CE is then handed a Socket and the Output- and InputStreams
 * that belong to the new connection. The CE then uses these Streams to send and receive transmissions.
 * 
 * Once the Streams and the Socket are handed to the CE, the CESH terminates.
 * 
 * If no initial message of type TransmissionTypeEnum.CONNECTION_REQUEST is received for 10 seconds, the CESH times out and also terminates.
 *
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class ConnectionEndpointServerHandler extends Thread{
	
	/** Logger for this CESH */
	Log ceshLog;

	/** This Socket is the one that will be handed over to the CE after receiving the initial message containing the foreign IP:::PORT of the connection. */
	private Socket clientSocket;	
	/** This is the Outgoing Communication Line that will be handed over to the CE. */
	private ObjectOutputStream serverOut;	
	/** This is the Incoming Communication Line that will be used to receive the initial message and is then handed over to the CE. */
	private ObjectInputStream serverIn;	
	/** This variable is used to hold the initial message once it has been received. */
	private NetworkPackage receivedMessage;
	/** This will be set to the IP Address of the connecting parties ServerSocket based on the contents of the initial message. */
	private String remoteIP;	
	/** This will be set to the Port of the connection parties ServerSocket based on the contents of the initial message. */
	private int remotePort;	
	/** As long as this is true, the CESH will keep trying to receive a message that contains the info needed to connect back to the remote CEs ServerSocket. */
	private boolean settingUp = true;	
	
	/** For control flow, this flag indicates whether we have accepted an incoming connection request */
	private boolean acceptedRequest = false;
	/** If a connection request arrives on the input stream of the socket created by the ServerSocket.accept() method, we create a ConnectionEndpoint in response */
	private ConnectionEndpoint ce = null;
	/** Will be passed on to the CE created in response to an incoming connection request */
	private int localPort;
	/** Will be passed on to the CE created in response to an incoming connection request */
	private String localIP;
	/** Will be passed to the CE created in response to an incoming connection request, will be the name the created CE tells its partner in response */
	private String localName;
	/** The {@linkplain ConnectionManager} that created this CESH */
	private ConnectionManager ownerCM;

	/**
	 * Constructor.
	 * For details see the JavaDoc of this class.
	 * @param newClientSocket
	 * 		a client socket created by a ServerSockets .accept() method <br>
	 * 		the CESH will listen for a connection request on this, 
	 * 		and if one is received this will be passed on as the client socket for the newly created {@linkplain ConnectionEndpoint}
	 * @param owner
	 * 		the {@linkplain ConnectionManager} that this CESH was created in <br>
	 * 		may not be null
	 * @throws IOException 
	 * 		if an I/O Exception occurred trying to construct an internal ObjectInputStream from the clientsocket's InputStream
	 */
	ConnectionEndpointServerHandler(Socket newClientSocket, ConnectionManager owner) throws IOException {
		clientSocket = newClientSocket;
		this.ownerCM = owner;
		this.localName = owner.getLocalName();
		
		this.ceshLog = new Log("CESH Logger (Owner: " + owner.getLocalName() + ":" + owner.getLocalPort() + ")", LogSensitivity.WARNING);
	}
	
	@Override
	public void run() {
		try {
			serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
			serverIn = new ObjectInputStream(clientSocket.getInputStream());
			while(settingUp) {
				
				//Create TimeOut
				NetworkTimeoutThread ntt = new NetworkTimeoutThread(3000, this, this.getClass().getMethod("terminateThread"));
				ntt.start();

				if((receivedMessage = (NetworkPackage) serverIn.readObject()) != null) {
					ceshLog.logInfo("[CESH " + localName + "] Received a Message: -.-"+ receivedMessage.getType().toString() + " - " + receivedMessage.getMessageArgs() +"-.-");
					
					//Create new CE
					if(receivedMessage.getType() == TransmissionTypeEnum.CONNECTION_REQUEST) {
						ntt.abortTimer();

						remoteIP = receivedMessage.getMessageArgs().localIP();
						remotePort = receivedMessage.getMessageArgs().localPort();

						/*
						 * Set the remote name and pk associated with the CE.
						 */
						String remoteName;
						// If the owner CM has a commlist, check it
						CommunicationList commList = ownerCM.getCommList();
						if (commList != null) {
							Contact dbEntry = commList.query(remoteIP, remotePort);
							if(dbEntry != null && !remoteIP.equals("127.0.0.1") && !remoteIP.equals("localhost")) {
								// Set the values accordingly, if the commlist has an entry for that IP:Port pair
								ceshLog.logInfo("Found pre-existing DB Entry that had matching IP:PORT to new connection request. Using Name and Sig from DB.");
								remoteName = dbEntry.getName();
							} else { // otherwise, no pk & set remote name based on message args
								remoteName = receivedMessage.getMessageArgs().userName();
							}
						} else {
							remoteName = receivedMessage.getMessageArgs().userName();
						}

						ce = ownerCM.createNewConnectionEndpoint(remoteName, clientSocket, serverOut, serverIn, remoteIP, remotePort);
						ce.setRemoteName(remoteName);
						settingUp = false;
						acceptedRequest = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}


	/**
	 * This stops the thread.
	 */
	public void terminateThread() {
		ceshLog.logInfo("Terminating ConnectionEndpointHandlerThread!");
		settingUp = false;
		this.interrupt();
	}

	/**
	 * @return true iff a connection request was accepted and a {@linkplain ConnectionEndpoint} was successfully created
	 */
	public boolean acceptedRequest() {
		return acceptedRequest;
	}

	
}
