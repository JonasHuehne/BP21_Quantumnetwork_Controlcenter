package exceptions;

/**
 * Thrown if a message of any type could not be decrypted. <br>
 * Usually wraps another Exception.
 * @author Sasha Petri
 */
public class CouldNotDecryptMessageException extends Exception {

	/**
	 * Constructor.
	 * @param message
	 * 		the message
	 * @param cause
	 * 		the cause, may be null if nonexistant / unknown
	 */
	public CouldNotDecryptMessageException(String message, Exception cause) {
		super(message, cause);
	}
}
