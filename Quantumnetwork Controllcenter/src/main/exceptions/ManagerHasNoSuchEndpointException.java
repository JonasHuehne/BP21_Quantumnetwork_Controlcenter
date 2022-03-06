package exceptions;

import networkConnection.ConnectionEndpoint;
import networkConnection.ConnectionManager;

/**
 * Intended for use in the {@linkplain ConnectionManager} class.
 * Throw this if you wish to indicate that a method did not execute as expected, 
 * <i>because</i> a specified {@linkplain ConnectionEndpoint} could not be found in the {@linkplain ConnectionManager}. <br>
 * @author Sasha Petri
 */
public class ManagerHasNoSuchEndpointException extends Exception {

	private static final long serialVersionUID = 7915045023966285257L;

	/**
	 * Constructs a new Exception with a message.
	 * @param name
	 * 		name of the connection endpoint that could not be found
	 */
	public ManagerHasNoSuchEndpointException(String name) {
		super("No " + ConnectionEndpoint.class.getCanonicalName() + " with the name " + name + " could be found in the ConnectionManager.");
	}
}
