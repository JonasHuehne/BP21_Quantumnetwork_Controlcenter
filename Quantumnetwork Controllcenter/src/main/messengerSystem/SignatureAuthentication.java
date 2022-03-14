package messengerSystem;

import exceptions.NoValidPublicKeyException;

/**
 * Interface for the general Methods to authenticate a message or communication chanel
 * @author Sarah Schumann
 */
public interface SignatureAuthentication {

    /**
     * Method to create a signature for a message using the designated private key
     * from the file named in the configurations
     * @param message the message to be signed with the private key
     * @return the signed message as a byte array; null if Error
     */
    byte[] sign (final byte[] message);

    /**
     * Method to verify a message with a signature, given a message, the signature and the sender name
     * (takes the public key from the corresponding entry in the communication list or the CE)
     * @param message the received signed message (without the signature)
     * @param receivedSignature the received signature
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     */
    boolean verify (final byte[] message, final byte[] receivedSignature, final String sender);

    /**
     * Generates a key pair for signing messages
     * (calls the other generateSignatureKeyPair Method with default parameters)
     * uses the default file name, specified by the class
     * deletes the key files if there are any with the same name (the default file name)
     * sets the created Key Pair as new own standard keys
     * deletes the currently set standard keys (even if they don't have the default file name)
     * @return true if it worked, false if error
     */
    boolean generateSignatureKeyPair ();

    /**
     * Generates a key pair for signing messages, using the chosen name for the key files
     * (public key as .pub file, private key as .key file)
     * @param keyFileName name for the created Key Pair
     * @param setAsKeyFile if true, sets the created Key Pair as new own standard keys
     * @param deleteCurrent if true, deletes the currently set standard keys
     * @param overwrite if true, any existing file with the same name will be overwritten
     * @return true if it worked, false if error
     */
    boolean generateSignatureKeyPair (String keyFileName, boolean setAsKeyFile,
                                                    boolean deleteCurrent, boolean overwrite);

    /**
     * Method to delete the key pair currently set as default key file
     * @return true if the deleting worked or already no default file set, false if error
     */
    boolean deleteSignatureKeys();

    /**
     * Method to delete the key file with the given file
     * @param keyFileName the name of the key file to be deleted
     * @return true if the deleting worked or the file didn't exist, false if error
     */
    boolean deleteSignatureKey(String keyFileName);

    /**
     * Method to set the private key file to be used in {@link #sign(String)}
     * @param keyFileName the name of the key file to set as standard private key
     *                    including the file name extension;
     *                    accepts "" (an empty string) as input for setting it to no key
     * @return true if it worked, false otherwise
     */
    boolean setPrivateKey (String keyFileName);

    /**
     * Method to set the public key to be used by the communication partner in {@link #verify(String, String, String)}
     * @param keyFileName the name of the key file to set as standard public key
     *                    including the file name extension
     *                    accepts "" (an empty string) as input for setting it to no key
     * @return true if it worked, false otherwise
     */
    boolean setPublicKey (String keyFileName);

    /**
     * Method to check if a valid key pair is currently set
     * @return true if there currently is a valid key pair set, false if not or error
     */
    boolean existsValidKeyPair();

}
