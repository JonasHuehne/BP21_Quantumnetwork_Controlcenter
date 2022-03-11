package networkConnection;

/**This Enum is used to select what method to use when sending a message via the gui.
 * 
 * @author Jonas Huehne
 *
 */
public enum ConnectionType {
	/** Messages are not authenticated or encrypted*/
	UNSAFE,	
	/** Messages are authenticated, but not encrypted */
	AUTHENTICATED,	
	/** Messages are authenticated and encrypted */
	ENCRYPTED	

}
