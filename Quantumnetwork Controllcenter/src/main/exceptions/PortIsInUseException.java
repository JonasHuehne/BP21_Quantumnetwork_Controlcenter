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
	 * 		entirely custom message
	 */
	public PortIsInUseException(String message) {
		super(message);
	}
}
