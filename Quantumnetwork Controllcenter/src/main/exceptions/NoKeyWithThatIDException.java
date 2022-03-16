package exceptions;

/**
 * Thrown if an attempt is made to access a key with a given ID,
 * but no key with that ID exists in the key store.
 * @author Sasha Petri
 */
public class NoKeyWithThatIDException extends Exception {

	private static final long serialVersionUID = -4910909194940562699L;

	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public NoKeyWithThatIDException(String message) {
		super(message);
	}
	
	
}
