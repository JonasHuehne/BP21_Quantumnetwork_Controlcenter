package main.messengerSystem;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

import main.encryptionDecryption.AES256;
import main.frame.QuantumnetworkControllcenter;
import main.keyStore.KeyStoreDbManager;
import main.keyStore.KeyStoreObject;
import main.networkConnection.ConnectionManager;
import main.networkConnection.ConnectionState;
import main.networkConnection.NetworkPackage;

/**High Level Message System. Contains methods for sending and receiving messages without dealing with low-level things.
 * Select active connectionEndpoint and start sending and receiving messages!
 * The first connectionEndpoint being created is automatically selected as active.
 *
 * @author Jonas HÃ¼hne, Sarah Schumann
 *
 */
public class MessageSystem {
	
	public static ConnectionManager conMan;

	/**This sends a message on the currently active connection. No confirmation is expected from the recipient.
	 *
	 * @param message the message to be sent on the active connection
	 */
	public static void sendMessage(String connectionID,String message) {

		if(conMan == null) {
			System.out.println("WARNING: Tried to send a message via the MessageSystem before initializing the QuantumnetworkControllcenter, thereby setting the connectionManager Reference.");
			return;
		}

		ConnectionState state = conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			conMan.sendMessage(connectionID, "msg", message);
		}
		else {
			System.out.println("[" + connectionID + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
	}

	/**Similar to sendMessage but allows for custom prefix. Used for internal system calls via the net.
	 *
	 * @param signal used as message prefix, should be one of the cases inside ConnectionEndpoint.java -> processMessage()
	 */
	public static void sendSignal(String connectionID, String signal) {
		if(conMan == null) {
			System.out.println("WARNING: Tried to send a message via the MessageSystem before initializing the QuantumnetworkControllcenter, thereby setting the connectionManager Reference.");
			return;
		}

		ConnectionState state = conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			conMan.sendMessage(connectionID, signal, signal);
		}
		else {
			System.out.println("[" + connectionID + "]: Sending of Confirm-Message: " + signal + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
	}


	/**This sends a message and the recipient is going to echo the message back to us.
	 *
	 * @param message the message to be sent on the active connection
	 * @return returns True if the confirmation of the message has been received, False if it times out.
	 */
	public static boolean sendConfirmedMessage(String connectionID, String message) {
		//System.out.println("[" + connectionID + "]: Sending Confirm-Message: " + message);
		ConnectionState state = QuantumnetworkControllcenter.conMan.getConnectionState(connectionID);
		if(state == ConnectionState.CONNECTED) {
			//Send message
			QuantumnetworkControllcenter.conMan.sendMessage(connectionID, "confirm", message);
			boolean waitForConfirmation = true;
			Instant startWait = Instant.now();
			Instant current;
			//Wait for confirmation
			System.out.println("[" + connectionID + "]: Starting to wait for Message Confirmation!");
			while(waitForConfirmation) {
				current = Instant.now();
				if(Duration.between(startWait, current).toSeconds() <= 10 && QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getConfirmations().contains(message)) {
					waitForConfirmation = false;
					System.out.println("[" + connectionID + "]: Message Confirmation received!");
					QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).clearConfirmation(message);
					return true;
				}
				else if (Duration.between(startWait, current).toSeconds() > 10) {
					waitForConfirmation = false;
					return false;
				}
			}
			return false;
		}
		else {
			System.out.println("[" + connectionID + "]: Sending of Confirm-Message: " + message + " aborted, because the ConnectionEndpoint was not connected to anything!");
		}
		return false;

	}

	/**reads the oldest Message available and removes it from the stack(actually a queue)
	 *
	 * @return the oldest message
	 */
	public static String readReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).readMessageFromStack().getContent();
	}

	/**returns the oldest message, but does not remove it from the queue
	 *
	 * @return the oldest message that was received and not yet read(removed)
	 */
	public static NetworkPackage previewReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).peekMessageFromStack();
	}

	/**returns the last message that was received but does not remove it from the queue
	 *
	 * @return the latest message
	 */
	public static String previewLastReceivedMessage(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).peekLatestMessageFromStack();
	}

	/**Returns the number of messages that are on the stack and waiting to be read(removed).
	 *
	 * @return the number of messages.
	 */
	public static int getNumberOfPendingMessages(String connectionID) {
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).sizeOfMessageStack();
	}

	/**Returns a linkedList of all un-read messages
	 *
	 * @return the list of unread messages.
	 */
	public static LinkedList<String> getAllReceivedMessages(String connectionID){
		return QuantumnetworkControllcenter.conMan.getConnectionEndpoint(connectionID).getMessageStack();
	}



	/**
	 * sends a signed message
	 * (currently implemented as sending the message and the signature separately)
	 * @param message the message to be sent
	 * @return true if the sending of both messages worked, false otherwise
	 */
	public static boolean sendAuthenticatedMessage(String connectionID, final String message) {
		String signature = QuantumnetworkControllcenter.authentication.sign(message);
		boolean res1 = sendConfirmedMessage(connectionID, message);
		boolean res2 = sendConfirmedMessage(connectionID, signature);
		return res1 && res2;
	}

	/**
	 * sends a signed message with encrypted tex
	 * 
	 * @param connectionID the ID of the reciever
	 * @param message the message to be sent
	 * @return true if the sending of the message worked, false otherwise
	 */
	public static boolean sendEncryptedMessage(String connectionID, final String message) {
		
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byte[] byteKey = keyObject.getKeyBuffer();
		
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		
		/*
		 * TODO 
		 * hier entsteht noch ziemliches durcheinander!
		 * Möglichkeiten das sauberer zu lösen:
		 * - keys in der DB direct in passender Länge speichern 
		 * - dem folgend eine möglichkeit den entsprechenden key als used zu markieren oder besser:
		 * 	 beim erhalten des Keys diret als used markieren
		 */
		
		String encrypted = AES256.encrypt(message, byteKey);
		
		return sendAuthenticatedMessage(connectionID, encrypted);		
	}
	
	/**
	 * receives a signed message
	 * (currently implemented as receiving two messages, first the message, then the signature)
	 * @return the received message as string, null if error none or if result of verify was false
	 */
	public static String readAuthenticatedMessage(String connectionID) {
		Instant startWait = Instant.now();
		while(getNumberOfPendingMessages(connectionID) < 2) {
			Instant current = Instant.now();
			if(Duration.between(startWait, current).toSeconds() > 10) {
				return null;
			}
		}
		String message = readReceivedMessage(connectionID);
		String signature = readReceivedMessage(connectionID);
		if(QuantumnetworkControllcenter.authentication.verify(message, signature, connectionID)) {
			return message;
		}
		return null;
	}
	
	public static String readEncryptedMessage(String connectionID) {
		String encrypted = readAuthenticatedMessage(connectionID);
		
		//getting key
		KeyStoreObject keyObject = KeyStoreDbManager.getEntryFromKeyStore(connectionID);
		byte[] byteKey = keyObject.getKeyBuffer();
				
		//marking key as used
		KeyStoreDbManager.changeKeyToUsed(connectionID);
		
		/*
		 * TODO
		 * 
		 * auch hier herrscht noch Durcheinander und Unklarheiten:
		 * -wie genau wird sich auf einen key geeinigt?
		 *  wird das über eine vorherige message gelöst 
		 *  oder wird vom KeyStore eine methode implementiert bei der immer der älteste unused key zurückgegeben wird?
		 *  
		 * Wenn sich vorher auf den key geeinigt wird muss noch ein paramete key hinzugefügt werden!
		 */
		
		//decrypting the message and then returning it
		return AES256.decrypt(encrypted, byteKey);
	}

}
