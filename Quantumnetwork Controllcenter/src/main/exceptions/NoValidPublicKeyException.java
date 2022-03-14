package exceptions;

public class NoValidPublicKeyException extends Exception {

    public NoValidPublicKeyException (String name) {
        super("No valid public key for the authentication exists for the connection " + name + ".");
    }

}
