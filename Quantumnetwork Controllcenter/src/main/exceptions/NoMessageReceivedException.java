package exceptions;

/**
 * Intended to be thrown by methods of MessageSystem (or related classes) if a message was awaited,
 * but did not come in, for example because a timeout occured.
 * @author Sasha Petri
 */
public class NoMessageReceivedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7736962968892128956L;

}
