package networkConnection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//A temporary method that inits the ConnectionManager inplace of the regular framework.
public class TempMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//System.out.println("Creating new ConnectionManager1");
		ConnectionManager cm = new ConnectionManager("192.168.0.73");
		//System.out.println("Creating new Connection in CM1");
		
		
		//Test
		
		//Testing, not part of the actual Program!
		ConnectionEndpoint ce = cm.createNewConnectionEndpoint("UserA", 2303);

		
		//ce.EstablishConnection("192.168.0.52", 3303);
		//ce.waitForMessage();
		//ce.pushMessage("Hallo Laptop, ich bin der Desktop.");
		
		

		
		
		

		
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
