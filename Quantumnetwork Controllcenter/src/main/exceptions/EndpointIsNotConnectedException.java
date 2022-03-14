package exceptions;

import networkConnection.ConnectionEndpoint;

/**
 * Intended to be thrown to indicate that an action (e.g. sending a message) failed
 * because the {@linkplain ConnectionEndpoint} that was intended to perform it was not connected to its partner.
 * @author Sasha Petri
 */
public class EndpointIsNotConnectedException extends Exception {

	private static final long serialVersionUID = 6115323110235376697L;

	/**
	 * Constructor for Exception with a message.
	 * @param name
	 * 		name / id of the {@linkplain ConnectionEndpoint} that could not send a message
	 * @param actionThatFailed
	 * 		description of the action that could not be performed, e.g. "send a message"
	 */
	public EndpointIsNotConnectedException(String name, String actionThatFailed) {
		super(ConnectionEndpoint.class.getCanonicalName() + " with ID " + name + " could not " 
				+ actionThatFailed + " because it was not connected to its communication partner");
	}
	
	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public EndpointIsNotConnectedException(String message) {
		super(message);
	}
}
