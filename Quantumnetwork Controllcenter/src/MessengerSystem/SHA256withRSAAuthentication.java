package MessengerSystem;

import communicationList.DbObject;
import frame.QuantumnetworkControllcenter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * class providing the methods necessary for authentication
 * @author Sarah Schumann
 */
public class SHA256withRSAAuthentication implements Authentication {

    private static final String KEY_PATH = System.getProperty("user.dir")
            + File.separator + "SignatureKeys" + File.separator;

    private static final String KEY_FILE_NAME = "signature";

    /**
     * a method to check, whether the folder for the signature keys already exists
     * creates it, if not
     */
    private static void checkFolder () {
        try {
            if (!Files.exists(Path.of(KEY_PATH))) {
                Files.createDirectory(Path.of(KEY_PATH));
            }
        } catch (Exception e) {
            System.err.println("Error while creating the SignatureKeys directory: " + e.getMessage());
        }
    }

    /**
     * method to create a signature for a message using the designated private key
     * @param message the message to be signed with the private key
     * @return the signed message as a String; null if Error
     */
    @Override
    public String sign (final String message) {
        try {
            checkFolder();
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get PrivateKey object from File
            PrivateKey privateKey = getPrivateKeyFromFile();
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
     * @throws IllegalArgumentException if sender null or does not exist, or no Signature Key for sender
     */
    @Override
    public boolean verify (final String message, final String receivedSignature,
                           final String sender) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get public key of sender from the db
            DbObject senderEntry = QuantumnetworkControllcenter.communicationList.query(sender);
            if (senderEntry == null) {
                throw new IllegalArgumentException("Sender not found Communication List");
            }
            String pubKey = senderEntry.getSignatureKey();
            if (pubKey == null || pubKey.equals("")) {
                throw new IllegalArgumentException
                        ("No Public signature Key found in Database for " + sender);
            }
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
    private PublicKey getPublicKeyFromString (final String key) {
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
     * method to get the private key from the default private key file in the SignatureKeys folder
     * @return a PrivateKey object created from the key in the file, null if error
     */
    private PrivateKey getPrivateKeyFromFile () {
        try {
            checkFolder();
            if(!Files.exists(Path.of(KEY_PATH + KEY_FILE_NAME + ".key"))) {
                System.err.println("Error while creating a private key from the signature key file: "
                        + "no signature key file found");
                return null;
            }
            String key = new String (Files.readAllBytes
                    (Path.of(KEY_PATH + KEY_FILE_NAME + ".key")));
            String keyString = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
            return kf.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            System.err.println("Error while creating a private key from the signature key file: " + e.getMessage());
            return null;
        }
    }

    /**
     * method to read a public key from the specified file in the SignatureKeys folder
     * and return it as a string
     * @param fileName the name of the key file
     * @return the public key from the file as a string (without the beginning and end)
     */
    public static String readPublicKeyStringFromFile (String fileName) {
        try {
            checkFolder();
            String key = new String (Files.readAllBytes
                    (Path.of(KEY_PATH + fileName + ".pub")));
            return key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");
        } catch (Exception e) {
            System.err.println("Error while creating a public key string from the input file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to delete the key pair currently set as default key file
     * (private key should be a .key, public key a .pub)
     * (static for now)
     * @return true if the deleting worked, false if no default or error
     */
    public static boolean deleteSignatureKeys() {
        try {
            checkFolder();
            Files.delete(Path.of(KEY_PATH + KEY_FILE_NAME + ".key"));
            Files.delete(Path.of(KEY_PATH + KEY_FILE_NAME + ".pub"));
            return true;
        } catch (Exception e) {
            System.err.println("Problem deleting the signature keys: " + e.getMessage());
            return false;
        }
    }

    /**
     * generates a key pair for signing messages
     * (both name "signature", private key as .key, public key as .pub)
     * deletes the current key files if there are any with the default name
     * (static for now)
     * @return true if it worked, false is name already used or error
     */
    public static boolean generateSignatureKeyPair () {
        try {
            checkFolder();
            if (Files.exists(Path.of(KEY_PATH + KEY_FILE_NAME + ".key"))) {
                deleteSignatureKeys();
            }
            KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
            kpGenerator.initialize(2048);
            KeyPair keyPair = kpGenerator.generateKeyPair();
            PublicKey pub = keyPair.getPublic();
            PrivateKey pvt = keyPair.getPrivate();
            Files.write(Path.of(KEY_PATH + KEY_FILE_NAME + ".key"),
                    Base64.getEncoder().encode(pvt.getEncoded()));
            Files.write(Path.of(KEY_PATH + KEY_FILE_NAME + ".pub"),
                    Base64.getEncoder().encode(pub.getEncoded()));
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating a key pair: " + e.getMessage());
            return false;
        }
    }
}
