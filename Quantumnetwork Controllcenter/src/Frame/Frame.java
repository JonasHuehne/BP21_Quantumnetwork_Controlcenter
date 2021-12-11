package Frame;

/*
 * Frame Class to run QuantumnetworkControllcenter
 */
public class Frame {

	/*
	 * Main-method to run QuantumnetworkControllcenter
	 */
	public static void main(String[] args) {
		
		QuantumnetworkControllcenter QnCc =new QuantumnetworkControllcenter()  ;
		
		System.out.println("Run QuantumnetworkControllCenter initialisation");
		
		QnCc.initialize();
		
	}
	
}

