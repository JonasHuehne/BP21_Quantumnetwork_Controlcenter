package networkConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import frame.QuantumnetworkControllcenter;
import messengerSystem.MessageSystem;

public class ConnectionEndpointServerHandler extends Thread{

	private Socket clientSocket;
	private ObjectOutputStream serverOut;
	private ObjectInputStream serverIn;
	private NetworkPackage receivedMessage;
	private String remoteIP;
	private int remotePort;
	
	ConnectionEndpointServerHandler(Socket newClientSocket, String targetIP, int targetPort) {
		clientSocket = newClientSocket;
		remoteIP = targetIP;
		remotePort = targetPort;
	}
	
	public void run() {
		System.out.println("-.-Starting to handle ConnectionEndpoint-.-");
		try {
			serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
			serverIn = new ObjectInputStream(clientSocket.getInputStream());
			System.out.println("-.-Trying to receive first Transmission...-.-");
			
			while(true) {
				if((receivedMessage = (NetworkPackage) serverIn.readObject()) != null) {
					System.out.println("-.-Received following Transmission:-.-");
					System.out.println("-.-"+ receivedMessage.getHead().toString() + " - " + receivedMessage.getTypeArg() +"-.-");
					
					//Create new CE
					if(receivedMessage.getHead() == TransmissionTypeEnum.CONNECTION_REQUEST) {
						System.out.println("-.-Creating new CE in responce to the ConnectionRequest");
						
						QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ResponceCE" + MessageSystem.generateRandomMessageID(), clientSocket, serverOut, serverIn, remoteIP, remotePort);
					}
					
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
