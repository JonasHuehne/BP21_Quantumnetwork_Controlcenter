package exceptions;

import networkConnection.ConnectionManager;

/**
 * Used to indicate that a port is already in use by a {@linkplain ConnectionManager}.
 * @author Sasha Petri
 *
 */
public class PortIsInUseException extends Exception {

	private static final long serialVersionUID = -136838864151620849L;

	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public PortIsInUseException(String message) {
		super(message);
	}
}
