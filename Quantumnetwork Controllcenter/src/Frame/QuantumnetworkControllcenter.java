package Frame;

/*
 * Main Class of QuantumnetworkControllcenter
 */
public class QuantumnetworkControllcenter {
	
	
	/*
	 * Method to initialize a Quantumnetwork Controllcenter
	 */
	public void initialize() {
		
		System.out.println(buildNetwork());
		
		System.out.println(runKeymanager());
		
		//TODO add initialization of further Classes
		
		System.out.println("QuantumnetworkControllcenter initialized");
	}
	
	
	
	/*
	 * Dummy Method to simulate initialization of Network
	 */
	public String buildNetwork() {
		
		return "Network build";
		
	}
	
	
	
	/*
	 * Dummy Method to simulate initialization of Keymanager
	 */
	public String runKeymanager() {
		
		return "Keymanager started";
		
	}
	
}
