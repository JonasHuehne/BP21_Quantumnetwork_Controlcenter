package exceptions;

/**
 * Thrown if an attempt is made to access a key with a given ID,
 * but no key with that ID exists in the key store.
 * @author Sasha Petri
 */
public class NoKeyWithThatIDException extends Exception {

	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		entirely custom message
	 */
	public NoKeyWithThatIDException(String message) {
		super(message);
	}
	
	
}
