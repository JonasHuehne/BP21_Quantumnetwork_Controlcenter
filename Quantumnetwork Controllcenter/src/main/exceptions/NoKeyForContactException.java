package exceptions;

/**
 * Thrown if an attempt is made to access a key for a contact,
 * but the contact has no key saved.
 * @author Sasha Petri
 */
public class NoKeyForContactException extends Exception {

	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		entirely custom message
	 */
	public NoKeyForContactException(String message) {
		super(message);
	}
	
	
}
