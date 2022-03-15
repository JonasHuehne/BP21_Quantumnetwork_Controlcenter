package exceptions;

import externalAPI.ExternalAPI;

/**
 * Thrown when a method in the {@linkplain ExternalAPI} class is called that would require
 * it to be initialized, while it is not initialized.
 * @author Sasha Petri
 */
public class ExternalApiNotInitializedException extends Exception {

	private static final long serialVersionUID = -8638024493975871337L;

	
	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public ExternalApiNotInitializedException(String message) {
		super(message);
	}
}
