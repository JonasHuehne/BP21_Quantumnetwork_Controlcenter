package MessengerSystem;

import java.security.Signature;
import java.security.PublicKey;
import java.security.PrivateKey;

public class Authentication {

    // temporary plain text keys for development and tests
    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    /**
     *
     * @param message the message to be signed with the private key
     * @return the signed message as a String; null if Error
     */
    public static String sign (String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // for now: static private key for dev and tests
            signature.initSign(privateKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            // return the signed message as String
            return new String(signature.sign());
        } catch (Exception e) {
            System.err.println("Error while signing: " + e.getMessage());
            return null;
        }
    }

    /**
     *
     * @param message the received signed message (only text without the signature)
     * @param receivedSignature the received signature
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     */
    public static boolean verify (String message, String receivedSignature, String sender) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // for now: static public key for dev and tests
            signature.initVerify(publicKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            // convert received signature from String to byte array
            byte[] recSig = receivedSignature.getBytes();
            // return result of verification
            return signature.verify(recSig);
        } catch (Exception e) {
            System.err.println("Error while verifying: " + e.getMessage());
            return false;
        }
    }
}
