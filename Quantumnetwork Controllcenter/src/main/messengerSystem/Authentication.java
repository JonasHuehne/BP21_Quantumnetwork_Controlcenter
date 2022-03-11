package messengerSystem;

/**
 * Interface for the general Methods to authenticate a message or communication chanel
 * @author Sarah Schumann
 */
public interface Authentication {

    /**
     * method to sign a message
     * @param message the message to be signed
     * @return the signature for the message
     */
    byte[] sign (final byte[] message);

    /**
     * method to verify a signature for a message
     * @param message the message
     * @param receivedSignature the signature
     * @param sender the sender from whom the message is
     * @return true if the signature is valid for the message from the sender, false otherwise
     */
    boolean verify (final byte[] message, final byte[] receivedSignature, final String sender);

}
