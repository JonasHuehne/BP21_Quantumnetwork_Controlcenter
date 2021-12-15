package networkConnection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//A temporary method that inits the ConnectionManager inplace of the regular framework.
public class TempMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//System.out.println("Creating new ConnectionManager1");
		ConnectionManager cm = new ConnectionManager("127.0.0.1");
		//System.out.println("Creating new Connection in CM1");
		
		
		//Test
		
		//Testing, not part of the actual Program!
		ConnectionEndpoint ce = cm.createNewConnectionEndpoint("UserA", 2303);
		ConnectionEndpoint ce2 = cm.createNewConnectionEndpoint("UserB", 3303);
		System.out.println(cm.getConnectionState("UserA"));
		ce2.waitForConnection();
		System.out.println(cm.getConnectionState("UserA"));
		ce.waitForConnection();
		System.out.println(cm.getConnectionState("UserA"));
		ce2.EstablishConnection("127.0.0.1", 2303);
		System.out.println(cm.getConnectionState("UserA"));
		ce2.waitForMessage();
		System.out.println(cm.getConnectionState("UserA"));
		//ce.EstablishConnection("localhost", 3303);
		//ce.waitForMessage();
		ce2.pushMessage("hallo, ich höre dich!");
		System.out.println(cm.getConnectionState("UserA"));
		ce.waitForMessage();
		System.out.println(cm.getConnectionState("UserA"));
		ce.pushMessage("toll, ich höre dich auch!");
		System.out.println(cm.getConnectionState("UserA"));
		ce2.waitForMessage();
		System.out.println(cm.getConnectionState("UserA"));
		cm.closeConnection("UserB");
		ce2.pushMessage("toll, ich höre dich auch!");
		System.out.println(cm.getConnectionState("UserA"));
		ce.waitForMessage();
		System.out.println(cm.getConnectionState("UserA"));
		ce.pushMessage("toll, ich höre dich auch!");
		

		
		
		

		
		//System.out.println("Creating new ConnectionManager2");
		//ConnectionManager cm2 = new ConnectionManager("127.0.0.1");
		//System.out.println("Creating new Connection in CM2");
		//ConnectionEndpoint ce2 = cm2.createNewConnectionEndpoint("TestVerbindungEmpfaenger", 3303);
		
		//System.out.println("Establishing Connection!");
		//ce.EstablishConnection("localhost", 3303);
		//ce2.waitForConnection();
		
		//cm2.sendMessage("TestVerbindungEmpfaenger", "Hallo, Testübertragung!");
		//ce.waitForMessage();

	}

}
