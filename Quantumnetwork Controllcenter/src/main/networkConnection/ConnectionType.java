package networkConnection;

/**This Enum is used to select what method to use when sending a message via the gui.
 * 
 * @author Jonas Huehne
 *
 */
public enum ConnectionType {
	UNSAFE,	//Default and unsafe Message.
	AUTHENTICATED,	//Authenticated Message that will be verified.
	ENCRYPTED	//Encrypted and Authenticated Message.

}
