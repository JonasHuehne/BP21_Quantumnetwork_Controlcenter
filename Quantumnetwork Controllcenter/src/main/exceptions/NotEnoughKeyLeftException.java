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
	 * @param keyID
	 * 		ID of the key with not enough bits left
	 * @param bitsWanted
	 * 		amount of key bits wanted
	 * @param bitsLeft
	 * 		amount of key bits left
	 */
	public NotEnoughKeyLeftException(String keyID, int bitsWanted, int bitsLeft) {
		super("Could not get " + bitsWanted + " bits of key from the key saved for the key with ID " + keyID + ". Only " + bitsLeft + " key bits remain.");
	}
	
	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public NotEnoughKeyLeftException(String message) {
		super(message);
	}
}
