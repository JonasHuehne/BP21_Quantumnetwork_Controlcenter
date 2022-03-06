package exceptions;


/**
 * Used to indicate that a port is already in use by a {@linkplain ConnectionManager}.
 * @author Sasha Petri
 *
 */
public class PortIsInUseException extends Exception {

	private static final long serialVersionUID = -136838864151620849L;

	public PortIsInUseException(String message) {
		super(message);
	}
}
