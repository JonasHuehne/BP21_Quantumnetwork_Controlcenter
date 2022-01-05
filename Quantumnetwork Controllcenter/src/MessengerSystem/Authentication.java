package MessengerSystem;

import CommunicationList.Database;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Signature;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * class providing the methods necessary for authentication
 */
public class Authentication {

    private static final String KEY_PATH = System.getProperty("user.dir")
            + File.separator + "SignatureKeys" + File.separator;

    /**
     * method to create a signature for a message using the designated private key
     * @param message the message to be signed with the private key
     * @return the signed message as a String; null if Error
     */
    public static String sign (final String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // for now: static private key for dev and tests
            // get PrivateKey object from String
            PrivateKey privateKey = getPrivateKeyFromFile("private_key.pem");
            signature.initSign(privateKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            byte[] sig = signature.sign();
            // convert signature into 'readable' string
            return new String(Base64.getEncoder().encode(sig));
        } catch (Exception e) {
            System.err.println("Error while signing: " + e.getMessage());
            return null;
        }
    }

    /**
     * method to verify a message with a signature, given a message, the signature and the sender name
     * (takes the public key from the corresponding entry in the communication list)
     * @param message the received signed message (only text without the signature)
     * @param receivedSignature the received signature as String
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     */
    public static boolean verify (final String message, final String receivedSignature, final String sender) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get public key of sender from the db
            String pubKey = Database.query(sender).getSignatureKey();
            // get PublicKey object from String
            PublicKey publicKey = getPublicKeyFromString(pubKey);
            signature.initVerify(publicKey);
            // convert message from String to byte array
            byte[] msg = message.getBytes();
            signature.update(msg);
            // convert receivedSignature to byte array
            byte[] recSig = Base64.getDecoder().decode(receivedSignature.getBytes());
            // return result of verification
            return signature.verify(recSig);
        } catch (Exception e) {
            System.err.println("Error while verifying: " + e.getMessage());
            return false;
        }
    }

    /**
     * method to generate a PublicKey object from a matching String
     * @param key the key as a string
     * @return the key as a PublicKey object, null if error
     */
    private static PublicKey getPublicKeyFromString (final String key) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
            return kf.generatePublic(publicKeySpec);
        } catch (Exception e) {
            System.err.println("Error while creating a public key from the input string: " + e.getMessage());
            return null;
        }
    }

    /**
     * method to get the private key from the specified file in the SignatureKeys folder
     * @param fileName the name of the key file (needs to be in pkcs8 format and not encrypted (for now))
     * @return a PrivateKey object created from the key in the file, null if error
     */
    private static PrivateKey getPrivateKeyFromFile (String fileName) {
        try {
            String key = new String (Files.readAllBytes
                    (Path.of(KEY_PATH + fileName)));
            String keyString = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
            return kf.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            System.err.println("Error while creating a private key from the input file: " + e.getMessage());
            return null;
        }
    }

    /**
     * method to read a public key from the specified file in the SignatureKeys folder
     * and return it as a string
     * @param fileName the name of the key file
     * @return the public key from the file as a string (without the begin and end)
     */
    public static String readPublicKeyStringFromFile (String fileName) {
        try {
            String key = new String (Files.readAllBytes
                    (Path.of(KEY_PATH + fileName)));
            return key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");
        } catch (Exception e) {
            System.err.println("Error while creating a public key string from the input file: " + e.getMessage());
            return null;
        }
    }
}
