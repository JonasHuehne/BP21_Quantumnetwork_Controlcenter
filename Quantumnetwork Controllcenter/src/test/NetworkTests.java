

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import messengerSystem.MessageSystem;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.TransmissionTypeEnum;

/**
 * Tests for the basic functionality of the network classes, in particular connection management.
 * Expects {@link Configuration} to work correctly.
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class NetworkTests {
	
	@BeforeAll
	public static void setup() {
		QuantumnetworkControllcenter.initialize(null);
	}
	
	@Nested
	/**
	 * Low Level Tests directly testing the {@link ConnectionEndpoint} class.
	 */
	class ConnectionEndpointTests {
		
		@Test
		public void singular_ce_behaves_as_expected() throws InterruptedException {
			String remoteAddr = "127.0.0.1";
			int	remotePort = 60200;
			// Create CE that tries to connect to a non-existant CE
			ConnectionEndpoint Alice = new ConnectionEndpoint("Alice", remoteAddr, remotePort);
			// It tries to connect
			assertEquals(ConnectionState.CONNECTING, Alice.reportState());
			// Wait until the connection attempt should time out
			Thread.sleep(3050);
			// It should no longer be trying to connect
			assertEquals(ConnectionState.CLOSED, Alice.reportState(), "CE should have stopped trying to connect. Is the time out set correctly?");
			
			// Assert that values are set correctly
			assertEquals(Alice.getLocalAddress(), Configuration.getProperty("UserIP"));
			assertEquals(Alice.getServerPort(), Integer.valueOf(Configuration.getProperty("UserPort")));
			assertEquals(Alice.getRemoteAddress(), remoteAddr);
			assertEquals(Alice.getRemotePort(), remotePort);
		}
		
	}
	
	@Nested
	/**
	 * Tests for the {@link ConnectionManager} class and the classes it uses to establish and manage connections.
	 */
	class ConnectionManagerTests {
		
		@Test
		public void basic_functionality_of_CM_works() {
			ConnectionManager conMan = QuantumnetworkControllcenter.conMan;
			
			// Connections can be created
			conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);
			assertEquals(1, conMan.returnAllConnections().size());
			assertNotNull(conMan.getConnectionEndpoint("Alice"));
			// Created connection immediately attempts to connect
			assertEquals(ConnectionState.CONNECTING, conMan.getConnectionState("Alice"));

			// Test that no two connections of the same name can be created
			conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);
			assertEquals(1, conMan.returnAllConnections().size());
			
			// No two connections to the same port and IP with different names can be created
			conMan.createNewConnectionEndpoint("Bob", "127.0.0.1", 60200);
			assertEquals(1, conMan.returnAllConnections().size(), "No new connection to the same address and port should have been created.");
			
			// Attempting to create the new connection should not have overriden Alice
			assertNotNull(conMan.getConnectionEndpoint("Alice"));
			
			// Assert that values are set correctly
			ConnectionEndpoint Alice = conMan.getConnectionEndpoint("Alice");
			assertEquals(Alice.getLocalAddress(), Configuration.getProperty("UserIP"));
			assertEquals(Alice.getServerPort(), Integer.valueOf(Configuration.getProperty("UserPort")));
			assertEquals(Alice.getRemoteAddress(), "127.0.0.1");
			assertEquals(Alice.getRemotePort(), 60200);
			
			// CE can be destroyed
			conMan.destroyConnectionEndpoint("Alice");
			assertNull(conMan.getConnectionEndpoint("Alice"));
			assertEquals(0, conMan.returnAllConnections().size());
			
			// Destroyed CE is closed
			assertEquals(ConnectionState.CLOSED, Alice);
			
			// Destroying multiple CE's works
			conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);
			conMan.createNewConnectionEndpoint("Bob", "127.0.0.2", 60200);
			assertEquals(2, conMan.returnAllConnections().size());
			conMan.destroyAllConnectionEndpoints();
			assertEquals(0, conMan.returnAllConnections().size());
		}
		
		public void can_create_cyclical_connection_and_send_messages() {
			// TODO <Sasha>: Due to the massive changes to the ConnectionManager and there only being one server port now,
			// the process of creating a cyclical connection has changed. I think I'd have to use ConnectionSwitchbox for that
			// but there is no documentation for it at the moment, so I don't know how to go about it.
			// I still think it would be good to have a local test for the sending and receiving of messages.
			assertFalse(true, "Not implemented yet.");
		}
		
		
		// TODO once proper Exception Handling is implemented
		public void methods_throw_no_such_connection_exception_when_appropriate() {
			assertFalse(true, "Not implemented yet.");
		}
		
	}
	
	@Nested
	/**
	 * Tests for some of the other functionalities provided by classes in the networkConnection package.
	 */
	class MiscNetworkTests {
		
		// TODO Add some tests here where appropriate
		
	}

}
