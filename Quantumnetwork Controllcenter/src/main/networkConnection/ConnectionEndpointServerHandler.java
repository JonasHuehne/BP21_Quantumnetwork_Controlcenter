package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

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
 * @author Jonas Huehne
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
	
	ConnectionEndpointServerHandler(Socket newClientSocket) {
		clientSocket = newClientSocket;

	}
	
	public void run() {
		System.out.println("-.-Starting to handle ConnectionEndpoint-.-");
		try {
			serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
			serverIn = new ObjectInputStream(clientSocket.getInputStream());
			System.out.println("-.-Trying to receive first Transmission...-.-");
			while(settingUp) {
				
				//Create TimeOut
				NetworkTimeoutThread ntt = new NetworkTimeoutThread(3000, this, this.getClass().getMethod("terminateThread"));
				ntt.start();
				
				if((receivedMessage = (NetworkPackage) serverIn.readObject()) != null) {
					System.out.println("-.-Received following Transmission:-.-");
					System.out.println("-.-"+ receivedMessage.getHead().toString() + " - " + receivedMessage.getTypeArg() +"-.-");
					
					//Create new CE
					if(receivedMessage.getHead() == TransmissionTypeEnum.CONNECTION_REQUEST) {
						ntt.abortTimer();
						System.out.println("-.-Creating new CE in responce to the ConnectionRequest");
						remoteIP = receivedMessage.getTypeArg().split(":::")[0];
						remotePort = Integer.valueOf(receivedMessage.getTypeArg().split(":::")[1]);
						System.out.println(receivedMessage.getTypeArg() + "---+++---");
						System.out.println(remoteIP + "---+++---");
						System.out.println(remotePort + "---+++---");
						QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ResponceCE" + MessageSystem.generateRandomMessageID(), clientSocket, serverOut, serverIn, remoteIP, remotePort);
						settingUp = false;
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
	
	public void terminateThread() {
		System.out.println("Terminating ConnectionEndpointHandlerThread!");
		settingUp = false;
		this.interrupt();
	}
	
	
}
