

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import exceptions.ConnectionAlreadyExistsException;
import exceptions.EndpointIsNotConnectedException;
import exceptions.IpAndPortAlreadyInUseException;
import exceptions.ManagerHasNoSuchEndpointException;
import exceptions.PortIsInUseException;
import messengerSystem.MessageSystem;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;
import networkConnection.ConnectionState;
import networkConnection.NetworkPackage;
import networkConnection.TransmissionTypeEnum;

/**
 * Tests for the basic functionality of the network classes, in particular connection management.
 * Expects {@link Configuration} to work correctly.
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class NetworkTests {
	
	@Nested
	/**
	 * Low Level Tests directly testing the {@link ConnectionEndpoint} class.
	 */
	class ConnectionEndpointTests {
		
		@Test
		public void singular_ce_behaves_as_expected() throws InterruptedException {
			String remoteAddr = "127.0.0.1";
			int	remotePort = 60200;
			// Create CE that tries to connect to a non-existent CE
			ConnectionEndpoint Alice = new ConnectionEndpoint("Alice", remoteAddr, remotePort, "127.0.0.1", 60400, "Bob");
			// It should no longer be trying to connect
			assertEquals(ConnectionState.CLOSED, Alice.reportState(), "CE should have stopped trying to connect. Is the time out set correctly?");
			
			// Assert that values are set correctly
			assertEquals(Alice.getLocalAddress(), "127.0.0.1");
			assertEquals(Alice.getServerPort(), 60400);
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
		/**
		 * None of this code should trigger any Exceptions.
		 * @throws IOException
		 * 		I/O error occurred trying to create a {@linkplain ConnectionManager}
		 * @throws PortIsInUseException
		 * 		tried to create a CM which uses a port already used by another CM
		 * @throws ManagerHasNoSuchEndpointException
		 * 		attempted sensitive access of a non-existant CE in a CM
		 * @throws ConnectionAlreadyExistsException
		 * 		attempted to create a CE with a non-unique ID in a CM
		 * @throws IpAndPortAlreadyInUseException
		 * 		attempted to create a CE with a non-unique IP:Port pair in a CM
		 */
		public void basic_functionality_of_CM_works() throws ConnectionAlreadyExistsException, IOException, PortIsInUseException, IpAndPortAlreadyInUseException {
			/*
			 * In this test we are "Bob", our partner is "Alice"
			 */
			
			int localServerPort = 60030; // port bob wishes to offer service on
			String localIP = "127.0.0.1"; // Bob's IP
			ConnectionManager conMan = new ConnectionManager(localIP, localServerPort, "Bob");
			
			// Connections can be created (Bob creating connection to Alice)
			conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);
			assertEquals(1, conMan.returnAllConnections().size());
			assertNotNull(conMan.getConnectionEndpoint("Alice"));

			// Test that no two connections of the same name can be created
			assertThrows(
					ConnectionAlreadyExistsException.class, 
					() -> {conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);});
			assertEquals(1, conMan.returnAllConnections().size());
	
			// Attempting to create the new connection should not have overridden Alice
			assertNotNull(conMan.getConnectionEndpoint("Alice"));
			
			// Assert that values are set correctly
			ConnectionEndpoint Alice = conMan.getConnectionEndpoint("Alice");
			assertEquals("127.0.0.1", Alice.getLocalAddress());
			assertEquals(localServerPort, Alice.getServerPort());
			assertEquals("127.0.0.1", Alice.getRemoteAddress());
			assertEquals(60200, Alice.getRemotePort());
			
			// CE can be destroyed
			try { conMan.destroyConnectionEndpoint("Alice");} catch (ManagerHasNoSuchEndpointException e) {}
			assertNull(conMan.getConnectionEndpoint("Alice"));
			assertEquals(0, conMan.returnAllConnections().size());
			
			// Destroyed CE is closed
			assertEquals(ConnectionState.CLOSED, Alice.reportState());
			
			// Destroying multiple CE's works
			conMan.createNewConnectionEndpoint("Alice", "127.0.0.1", 60200);
			conMan.createNewConnectionEndpoint("Bob", "127.0.0.2", 60200);
			assertEquals(2, conMan.returnAllConnections().size());
			conMan.destroyAllConnectionEndpoints();
			assertEquals(0, conMan.returnAllConnections().size());
		}
		
		@Test
		/**
		 * None of this code should trigger any Exceptions.
		 * @throws IOException
		 * 		I/O error occurred trying to create a {@linkplain ConnectionManager}
		 * @throws PortIsInUseException
		 * 		tried to create a CM which uses a port already used by another CM
		 * @throws ManagerHasNoSuchEndpointException
		 * 		attempted sensitive access of a non-existant CE in a CM
		 * @throws ConnectionAlreadyExistsException
		 * 		attempted to create a CE with a non-unique ID in a CM
		 * @throws IpAndPortAlreadyInUseException
		 * 		attempted to create a CE with a non-unique IP:Port pair in a CM
		 */
		public void can_create_cyclical_connection_and_close_them() throws IOException, PortIsInUseException, ConnectionAlreadyExistsException, ManagerHasNoSuchEndpointException, IpAndPortAlreadyInUseException {
			int serverPortAlice = 60020;
			int serverPortBob	= 60040;
			String ipAlice		= "127.0.0.1";
			String ipBob		= "127.0.0.1";
			
			ConnectionManager AliceCM = new ConnectionManager(ipAlice, serverPortAlice, "Alice"); // Used to Model PC of Alice
			ConnectionManager BobCM	  = new ConnectionManager(ipBob, serverPortBob, "Bob"); // Used to Model PC of Bob
			
			// Alice attempts to connect to Bob
			AliceCM.createNewConnectionEndpoint("Bob", ipBob, serverPortBob);
			
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Both should now have exactly one connection, to each other
			assertEquals(1, AliceCM.returnAllConnections().size());
			assertEquals(1, BobCM.returnAllConnections().size());
			
			assertNotNull(AliceCM.getConnectionEndpoint("Bob"));
			// The Name of Bob's partner should be Alice, because that's the name of Alice CM
			assertNotNull(BobCM.getConnectionEndpoint("Alice"));
			
			// Both Connection should have state connected
			assertEquals(ConnectionState.CONNECTED, AliceCM.getConnectionState("Bob"));
			assertEquals(ConnectionState.CONNECTED, BobCM.getConnectionState("Alice"));
			
			// Closing one connection also closes the other
			AliceCM.closeConnection("Bob");
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertEquals(ConnectionState.CLOSED, AliceCM.getConnectionState("Bob"));
			assertEquals(ConnectionState.CLOSED, BobCM.getConnectionState("Alice"));
			
		}
		
		@Test
		/**
		 * None of this code should trigger any exceptions.
		 * @throws IOException
		 * 		I/O error occurred trying to create a {@linkplain ConnectionManager}
		 * @throws PortIsInUseException
		 * 		tried to create a CM which uses a port already used by another CM
		 * @throws ManagerHasNoSuchEndpointException
		 * 		attempted sensitive access of a non-existant CE in a CM
		 * @throws ConnectionAlreadyExistsException
		 * 		attempted to create a CE with a non-unique ID in a CM
		 * @throws IpAndPortAlreadyInUseException
		 * 		attempted to create a CE with a non-unique IP:Port pair in a CM
		 * @throws EndpointIsNotConnectedException
		 * 		if trying to send a message from an endpoint that is not connected
		 */
		public void can_send_messages_along_cyclical_connection() 
				throws 	IOException, PortIsInUseException, ManagerHasNoSuchEndpointException, ConnectionAlreadyExistsException, 
						IpAndPortAlreadyInUseException, EndpointIsNotConnectedException {
			int serverPortAlice = 60021;
			int serverPortBob	= 60041;
			String ipAlice		= "127.0.0.1";
			String ipBob		= "127.0.0.1";
			
			ConnectionManager AliceCM = new ConnectionManager(ipAlice, serverPortAlice, "Alice"); // Used to Model PC of Alice
			ConnectionManager BobCM	  = new ConnectionManager(ipBob, serverPortBob, "Bob"); // Used to Model PC of Bob
			
			// Alice attempts to connect to Bob
			AliceCM.createNewConnectionEndpoint("Bob", ipBob, serverPortBob);

			
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Alice tries sending a message
			
			byte[] transmittedBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
			NetworkPackage examplePackage = new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, null, transmittedBytes, false);
			AliceCM.sendMessage("Bob", examplePackage);
			
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// assert the message arrived correctly
			ConnectionEndpoint BobsConnectionToAlice = BobCM.getConnectionEndpoint("Alice");
			assertEquals(1, BobsConnectionToAlice.getLoggedPackagesOfType(TransmissionTypeEnum.TEXT_MESSAGE).size());
			assertArrayEquals(transmittedBytes, BobsConnectionToAlice.getPackageLog().get(0).getContent());
		}
		
		
		@Test
		public void methods_throw_appropriate_exceptions() {
			
			// Trying to create two CM with the same port
			assertThrows(PortIsInUseException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60042, "Alice");
				ConnectionManager CM2 = new ConnectionManager("127.0.0.1", 60042, "Alice");
			});
			
			// Trying to insert two CEs of the same name into one CM
			assertThrows(ConnectionAlreadyExistsException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60043, "Bob");
				CM.createNewConnectionEndpoint("Alice", "127.0.0.1", 60040);
				CM.createNewConnectionEndpoint("Alice", "127.0.0.1", 60050);
			});
			
			// Trying to destroy a non-existant CE 
			assertThrows(ManagerHasNoSuchEndpointException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60045, "Alice");
				CM.destroyConnectionEndpoint("Bob");
			});
			
			// Trying to access state of non-existant CE
			assertThrows(ManagerHasNoSuchEndpointException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60046, "Alice");
				CM.getConnectionState("Bob");
			});
			
			// Trying to send a message when a CE is not connected
			assertThrows(EndpointIsNotConnectedException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60047, "Alice");
				CM.createNewConnectionEndpoint("Bob", "127.0.0.1", 34341);
				CM.sendMessage("Bob", new NetworkPackage(TransmissionTypeEnum.TEXT_MESSAGE, false));
			});
			
			// Trying to insert two CEs connecting to the same IP:Port pair into one CM
			// will fail in case same IP:Port pair is currently allowed to enable manual testing
			assertThrows(IpAndPortAlreadyInUseException.class, () -> {
				ConnectionManager CM = new ConnectionManager("127.0.0.1", 60044, "Charlie");
				CM.createNewConnectionEndpoint("Alice", "127.0.0.1", 60043);
				CM.createNewConnectionEndpoint("Bob", "127.0.0.1", 60043);
			}, "This failure is most likely caused by identical IP:Port pairs being allowed for manual testing purposes.");
		}
		
		/**
		 * Telling a ConnectionManager to stop waiting and then start waiting again leads to no issues with connection establishment.
		 * None of this code should trigger any exceptions.
		 * @throws IOException
		 * 		I/O error occurred trying to create a {@linkplain ConnectionManager}
		 * @throws PortIsInUseException
		 * 		tried to create a CM which uses a port already used by another CM
		 * @throws ManagerHasNoSuchEndpointException
		 * 		attempted sensitive access of a non-existant CE in a CM
		 * @throws ConnectionAlreadyExistsException
		 * 		attempted to create a CE with a non-unique ID in a CM
		 * @throws IpAndPortAlreadyInUseException
		 * 		attempted to create a CE with a non-unique IP:Port pair in a CM
		 */
		@Test
		public void restarting_wait_for_connections_works() throws IOException, PortIsInUseException, ConnectionAlreadyExistsException, IpAndPortAlreadyInUseException, ManagerHasNoSuchEndpointException {
			ConnectionManager CMAlice = new ConnectionManager("127.0.0.1", 60055, "Alice");
			ConnectionManager CMBob = new ConnectionManager("127.0.0.1", 60056, "Bob");
			
			if (!CMAlice.isWaitingForConnections()) CMAlice.waitForConnections();
			
			CMAlice.stopWaitingForConnections();
			assertFalse(CMAlice.isWaitingForConnections());
			
			CMAlice.waitForConnections();
			assertTrue(CMAlice.isWaitingForConnections());
			
			// Bob attempts to connect to Alice
			CMBob.createNewConnectionEndpoint("Alice", "127.0.0.1", 60055);
			// Alice should accept the connection
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			assertEquals(1, CMAlice.getConnectionsAmount());
			assertEquals(1, CMBob.getConnectionsAmount());
			
			assertEquals(ConnectionState.CONNECTED, CMAlice.returnAllConnections().values().iterator().next().reportState());
			assertEquals(ConnectionState.CONNECTED, CMBob.getConnectionState("Alice"));
			
			
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
