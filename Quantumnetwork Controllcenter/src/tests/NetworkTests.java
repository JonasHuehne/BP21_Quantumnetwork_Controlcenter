package tests;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import MessengerSystem.MessageSystem;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionState;

public class NetworkTests {
	
	QuantumnetworkControllcenter QCC = new QuantumnetworkControllcenter();

	
	@Test
	public void testConnectionEndpoints() {
		QuantumnetworkControllcenter.initialize();
		
		//Create 2 connectionEndpoints + invalid Duplicates
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest1", 2300);
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest2", 3300);
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest2", 4300);
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest3", 3300);
		assert(QuantumnetworkControllcenter.conMan.returnAllConnections().size() == 2);
		
		//Closing Connections without destroying the connectionEndpoints.
		QuantumnetworkControllcenter.conMan.closeAllConnections();
		assert(QuantumnetworkControllcenter.conMan.returnAllConnections().size() == 2);
		
		//Destroying one connectionEndpoint.
		QuantumnetworkControllcenter.conMan.destroyConnectionEndpoint("ceTest2");
		assert(QuantumnetworkControllcenter.conMan.returnAllConnections().size() == 1);
		
		//Destroying all connectionEndpoint.
		QuantumnetworkControllcenter.conMan.destroyAllConnectionEndpoints();
		assert(QuantumnetworkControllcenter.conMan.returnAllConnections().size() == 0);
	}
	
	
	@Test
	public void testConnecting() {
		
		//Setup and initial State-Check
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest1", 2300);
		ConnectionEndpoint ce1 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest1");
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest2", 3300);
		ConnectionEndpoint ce2 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest2");
		assert(ce1.reportState() == ConnectionState.CLOSED);
		assert(ce2.reportState() == ConnectionState.CLOSED);
		
		//Start waiting for connection
		ce1.waitForConnection();
		System.out.println(ce1.reportState() );
		
		//delay
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Check State
		assert(ce1.reportState() == ConnectionState.WAITINGFORCONNECTION);
		
		//Connection creation from other connectionEndpoint
		try {
			ce2.establishConnection("localhost", 2300);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Check State
		assert(ce1.reportState() == ConnectionState.CONNECTED);		
	}
	
	@Test
	public void testMessageLowLevel() {
		ConnectionEndpoint ce1 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest1");
		ConnectionEndpoint ce2 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest2");
		//Start checking for a Message
		ce2.listenForMessage();
		//Send a Message
		ce1.pushMessage("msg", "test Message 1");
		
		//Wait
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Check if Message was stored in Queue
		assert(ce2.sizeOfMessageStack() == 1);
		assert(ce2.sizeOfMessageStack() == 1);
		
		//Test against unintended access to original data.
		ce2.getMessageStack().clear();
		assert(ce2.sizeOfMessageStack() == 1);
		
		//Do the same for the ConnectionEndpointMap
		Map<String, ConnectionEndpoint> i = QuantumnetworkControllcenter.conMan.returnAllConnections();
		assert(i.size() == 2);
		i.clear();
		i = QuantumnetworkControllcenter.conMan.returnAllConnections();
		assert(i.size() == 2);
		
		//Close the connection again
		ce1.closeConnection(true);
		
		//Wait
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Check that both connectionEndpoints are closed down.
		assert(ce1.reportState() == ConnectionState.CLOSED);
		assert(ce2.reportState() == ConnectionState.CLOSED);
	}
	
	@Test
	public void testMessageHighLevel() {
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest8", 4300);
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest9", 5300);
		ConnectionEndpoint ce3 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest8");
		ConnectionEndpoint ce4 = QuantumnetworkControllcenter.conMan.getConnectionEndpoint("ceTest9");
		
		ce3.waitForConnection();
		try {
			ce4.establishConnection("localhost", 4300);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Test regular Messages
		ce4.listenForMessage();
		
		MessageSystem.sendMessage("ceTest8", "Test Message 3");
		MessageSystem.sendMessage("ceTest8", "Test Message 4");
		MessageSystem.sendMessage("ceTest8", "Test Message 5");
		
		//Wait
		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assert(MessageSystem.getNumberOfPendingMessages("ceTest9") == 3);
		//Test Message Content
		assert(MessageSystem.previewReceivedMessage("ceTest9").getContent().equals("Test Message 3"));
		//Test non-removing preview
		assert(MessageSystem.previewReceivedMessage("ceTest9").getContent().equals("Test Message 3"));
		//Test non-removing preview of latest message
		assert(MessageSystem.previewLastReceivedMessage("ceTest9").equals("Test Message 5"));
		//Test Message Read
		assert(MessageSystem.readReceivedMessage("ceTest9").equals("Test Message 3"));
		//Test removing-reading
		assert(MessageSystem.readReceivedMessage("ceTest9").equals("Test Message 4"));
		//Try sending confirmed Messages
		MessageSystem.sendConfirmedMessage("ceTest8", "Confirmed Test Message 1");
		
		//Check for no unintended confirmations on message stack
		assert(MessageSystem.getNumberOfPendingMessages("ceTest8") == 0);
		
		//Check Message Order after confirmed Message
		assert(MessageSystem.getNumberOfPendingMessages("ceTest9") == 2);
		assert(MessageSystem.readReceivedMessage("ceTest9").equals("Test Message 5"));
		assert(MessageSystem.readReceivedMessage("ceTest9").equals("Confirmed Test Message 1"));
		
		QuantumnetworkControllcenter.conMan.destroyAllConnectionEndpoints();
		assert(QuantumnetworkControllcenter.conMan.returnAllConnections().size() == 0);
	}

}
