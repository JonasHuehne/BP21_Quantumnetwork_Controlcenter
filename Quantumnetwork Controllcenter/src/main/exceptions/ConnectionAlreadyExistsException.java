package exceptions;

import networkConnection.ConnectionManager;

/**
 * Used by the {@linkplain ConnectionManager} when trying to create a connection
 * with a name that is the same as another connection already managed by that {@linkplain ConnectionManager}.
 * @author Sasha Petri
 */
public class ConnectionAlreadyExistsException extends Exception {

	/**
	 * Constructs a new Exception with a message.
	 * @param name
	 * 		name of the connection that already exists
	 */
	public ConnectionAlreadyExistsException(String name) {
		super("A connection with the name " + name + " already exists in this ConnectionManager.");
	}
	
}
