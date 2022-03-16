package exceptions;

import networkConnection.NetworkPackage;

/**
 * This Exception can be thrown if the verification of a {@linkplain NetworkPackage} failing is an unexpected scenario,
 * or when retuning null in the case of failure would be unclear (e.g. if a method verifies a package and then returns
 * its NetworkPackage.content, a null return <i>could</i> mean verification failed, or that the contents were null.
 * With this Exception, the method can directly tell the caller what happened.)
 * @author Sasha Petri
 *
 */
public class VerificationFailedException extends Exception {
	
	private static final long serialVersionUID = 7128800800931123107L;

	/**
	 * Constructor for Exception with a message.
	 * @param message
	 * 		entirely custom message
	 */
	public VerificationFailedException(String message) {
		super(message);
	}
	
}
