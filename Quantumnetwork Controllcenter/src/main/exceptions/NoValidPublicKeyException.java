package exceptions;

/**
 * Exception to be thrown if there is no public signature key for a connection
 * (mostly while trying to verify a message)
 * @author Sarah Schumann
 */
public class NoValidPublicKeyException extends Exception {

    /**
     * Throws an exception with the given name
     * @param name the name of the concerned connection endpoint
     */
    public NoValidPublicKeyException (String name) {
        super("No valid public key for the authentication exists for the connection " + name + ".");
    }

}
