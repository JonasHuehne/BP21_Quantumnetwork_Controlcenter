package exceptions;

import keyStore.KeyStoreDbManager;

/**
 * Thrown by the {@linkplain KeyStoreDbManager} when trying to get {@code n} bits of a key
 * but less than {@code n} bits are left.
 * @author Sasha Petri
 */
public class NotEnoughKeyLeftException extends Exception {

	private static final long serialVersionUID = 6425289752822873684L;

	/**
	 * Constructor.
	 * Generates a Message based on the parameters.
	 * @param contactName
	 * 		name of the contact with not enough key bits left
	 * @param bitsWanted
	 * 		amount of key bits wanted
	 * @param bitsLeft
	 * 		amount of key bits left
	 */
	public NotEnoughKeyLeftException(String contactName, int bitsWanted, int bitsLeft) {
		super("Could not get " + bitsWanted + " bits of key from the key saved for contact " + contactName + ". Only " + bitsLeft + " key bits remain.");
	}
	
	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		entirely custom message
	 */
	public NotEnoughKeyLeftException(String message) {
		super(message);
	}
}
