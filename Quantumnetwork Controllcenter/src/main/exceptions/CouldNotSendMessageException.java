package exceptions;

import messengerSystem.MessageSystem;

/**
 * Thrown by the {@linkplain MessageSystem} if a Message could not be sent for some reason. <br>
 * Generally, this wraps a lower level Exception.
 * @author Sasha Petri
 *
 */
public class CouldNotSendMessageException extends Exception {

	private static final long serialVersionUID = 8277273037829122223L;

	/**
	 * Constructor.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 * @param cause
	 * 		the cause, may be null if nonexistent / unknown
	 */
	public CouldNotSendMessageException(String message, Exception cause) {
		super(message, cause);
	}
}
