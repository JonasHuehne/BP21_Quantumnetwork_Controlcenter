package exceptions;

import keyGeneration.KeyGenerator;

/**
 * Thrown by the {@linkplain KeyGenerator} to indicate that a timeout occurred in response to a request for key generation. 
 * @author Sasha Petri
 *
 */
public class KeyGenRequestTimeoutException extends Exception {

	/**
	 * Constructor for Exception with a Message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public KeyGenRequestTimeoutException(String message) {
		super(message);
	}

	private static final long serialVersionUID = -4448050281341074788L;

}
