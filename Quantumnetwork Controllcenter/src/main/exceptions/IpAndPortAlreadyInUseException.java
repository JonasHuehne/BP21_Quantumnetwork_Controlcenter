package exceptions;

import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;

/**
 * To be thrown by {@linkplain ConnectionManager} if trying to create
 * a connection to an IP:Port pair that is already in use.
 * @author Sasha Petri
 */
public class IpAndPortAlreadyInUseException extends Exception {

	private static final long serialVersionUID = -392821520366214780L;

	/**
	 * Construct an Exception with a message detailing that no CE with the specified values could be constructed.
	 * @param IP
	 * 		remote IP for the CE
	 * @param port
	 * 		remote port for the CE 
	 */
	public IpAndPortAlreadyInUseException(String IP, int port) {
		super("Can not create a " + ConnectionEndpoint.class.getCanonicalName() + " with remote IP " + IP + " and remote port " + port + ". "
				+ "A " + ConnectionEndpoint.class.getCanonicalName() + " with these same values already exists in this " + ConnectionManager.class.getCanonicalName());
	}
}
