package tests;

import org.junit.jupiter.api.Test;

import frame.QuantumnetworkControllcenter;

public class NetworkTests {
	
	QuantumnetworkControllcenter QCC = new QuantumnetworkControllcenter();

	
	@Test
	public void testConnectionEndpoints() {
		QuantumnetworkControllcenter.initialize();
		
		//Create 2 connectionEndpoints
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest1", 2300);
		QuantumnetworkControllcenter.conMan.createNewConnectionEndpoint("ceTest2", 3300);
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

}
