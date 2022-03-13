package exceptions;

/**
 * This Exception can be thrown if it was not possible to get a key
 * for symmetric encryption. It usually wraps another Exception, such as 
 * {@linkplain NotEnoughKeyLeftException} or {@linkplain NoKeyWithThatIDException}.
 * @author Sasha Petri
 */
public class CouldNotGetKeyException extends Exception {

	private static final long serialVersionUID = -3287227638250420522L;

	/**
	 * Constructor.
	 * @param message
	 * 		the message
	 * @param cause
	 * 		the cause, may be null if nonexistant / unknown
	 */
	public CouldNotGetKeyException(String message, Exception cause) {
		super(message, cause);
	}
}
