package exceptions;

/**
 * Thrown if a message of any type could not be encrypted. <br>
 * Usually wraps another Exception.
 * @author Sasha Petri
 */
public class CouldNotEncryptMessageException extends Exception {

	private static final long serialVersionUID = -9033562097694662689L;

	/**
	 * Constructor.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 * @param cause
	 * 		the cause, may be null if nonexistent / unknown
	 */
	public CouldNotEncryptMessageException(String message, Exception cause) {
		super(message, cause);
	}
}
