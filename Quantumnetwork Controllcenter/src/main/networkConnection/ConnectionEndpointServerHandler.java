package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import communicationList.Contact;
import exceptions.ConnectionAlreadyExistsException;
import exceptions.IpAndPortAlreadyInUseException;
import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;

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

	private Socket clientSocket;	//This Socket is the one that will be handed over to the CE after receiving the initial message containing the foreign IP:::PORT of the connection.
	private ObjectOutputStream serverOut;	//This is the Outgoing Communication Line that will be handed over to the CE.
	private ObjectInputStream serverIn;	//This is the Incoming Communication Line that will be used to receive the initial message and is then handed over to the CE.
	private NetworkPackage receivedMessage;	//This variable is used to hold the initial message once it has been received.
	private String remoteIP;	//This will be set to the IP Address of the connecting parties ServerSocket based on the contents of the initial message.
	private int remotePort;	//This will be set to the Port of the connection parties ServerSocket based on the contents of the initial message.
	private boolean settingUp = true;	//As long as this is true, the CESH will keep trying to receive a message that contains the info needed to connect back to the remote CEs ServerSocket.

	private boolean acceptedRequest = false;
	private ConnectionEndpoint ce = null;

	
	/**
	 * Constructor.
	 * For details see the JavaDoc of this class.
	 * @param newClientSocket
	 * 		a client socket created by a ServerSockets .accept() method <br>
	 * 		the CESH will listen for a connection request on this, 
	 * 		and if one is received this will be passed on as the client socket for the newly created {@linkplain ConnectionEndpoint}
	 * @param localIP
	 * 		local IP that will be passed to the newly created  {@linkplain ConnectionEndpoint}
	 * @param localPort
	 * 		local port that will be passed to the newly created {@linkplain ConnectionEndpoint}
	 * @throws IOException 
	 * 		if an I/O Exception occurred trying to construct an internal ObjectInputStream from the clientsocket's InputStream
	 */
	ConnectionEndpointServerHandler(Socket newClientSocket) throws IOException {
		clientSocket = newClientSocket;

	}
	
	public void run() {
		try {
			serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
			serverIn = new ObjectInputStream(clientSocket.getInputStream());
			while(settingUp) {
				
				//Create TimeOut
				NetworkTimeoutThread ntt = new NetworkTimeoutThread(3000, this, this.getClass().getMethod("terminateThread"));
				ntt.start();

				if((receivedMessage = (NetworkPackage) serverIn.readObject()) != null) {
					System.out.println("CESH Received a Message: "+ receivedMessage.getHead().toString());
					
					//Create new CE
					if(receivedMessage.getHead() == TransmissionTypeEnum.CONNECTION_REQUEST) {
						ntt.abortTimer();
						remoteIP = receivedMessage.getTypeArg().split(":::")[0];
						remotePort = Integer.valueOf(receivedMessage.getTypeArg().split(":::")[1]);

						//Check if ContactDB contains the IP:PORT Pair already. If so, the Name and Sig is taken from the DB.
						String remoteName;
						Contact dbEntry = QuantumnetworkControllcenter.communicationList.query(remoteIP, remotePort);
						if(dbEntry != null && !remoteIP.equals("127.0.0.1") && !remoteIP.equals("localhost")) {
							System.out.println("Found pre-existing DB Entry that had matching IP:PORT to new connection request. Using Name and Sig from DB.");
							remoteName = dbEntry.getName();
						}else {
							remoteName = receivedMessage.getTypeArg().split(":::")[2];
						}
						ce = QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint(remoteName, clientSocket, serverOut, serverIn, remoteIP, remotePort);
						ce.setRemoteName(remoteName);
						settingUp = false;
						acceptedRequest = true;
					}
				}
				
				//Timeout after 10 Seconds
				if(Duration.between(startWait, current).toSeconds() <= 10) {
					System.out.println("TIMEOUT!");
					settingUp = false;
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
		} catch (ConnectionAlreadyExistsException e) {
			e.printStackTrace();
		} catch (IpAndPortAlreadyInUseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This stops the thread.
	 */
	public void terminateThread() {
		System.out.println("Terminating ConnectionEndpointHandlerThread!");
		settingUp = false;
		this.interrupt();
	}
	
	/**
	 * @return true iff a connection request was accepted and a {@linkplain ConnectionEndpoint} was successfully created
	 */
	public boolean acceptedRequest() {
		return acceptedRequest;
	}
	
	/**
	 * @return the ConnectionEndpoint created by this CESH accepting a request, may be null if no request was accepted
	 */
	public ConnectionEndpoint getCE() {
		return ce;
	}
	
}
