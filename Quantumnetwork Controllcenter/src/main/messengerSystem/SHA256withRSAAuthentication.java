package messengerSystem;

import communicationList.Contact;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Class providing the methods necessary for authentication
 * using SHA256 with RSA 2048
 * @author Sarah Schumann
 */
public class SHA256withRSAAuthentication implements Authentication {

    /**
     * The path to the folder for the signature keys, includes a file separator at the end
     */
    private static final String KEY_PATH = "SignatureKeys" + File.separator;

    /**
     * Default name for generating signature key files, without file name extension
     */
    private static final String DEFAULT_KEY_FILE_NAME = "signature";

    /**
     * Name of the field for the private key file name in the properties file
     */
    private static final String PRIVATE_KEY_PROP_NAME = "privateKeyFile";
    /**
     * File name of the currently used own private key file
     * should include the file name extension
     */
    private static String privateKeyFile = "";

    /**
     * Name of the field for the public key file name in the properties file
     */
    private static final String PUBLIC_KEY_PROP_NAME = "publicKeyFile";
    /**
     * File name of the currently used own public key file
     * should include the file name extension
     */
    private static String publicKeyFile = "";

    // accepted file name extensions are:
    // .pub .pem .key .der .txt or no extension
    private static final String KEY_FILENAME_SYNTAX =
            "(.+\\.pub)|(.+\\.pem)|(.+\\.key)|(.+\\.der)|(.+\\.txt)|(^[^.]+$)";

    /**
     * Constructor of the class, calls the methods to check
     * if the needed folders and files exist
     */
    public SHA256withRSAAuthentication() {
        privateKeyFile = Configuration.getProperty(PRIVATE_KEY_PROP_NAME);
        publicKeyFile = Configuration.getProperty(PUBLIC_KEY_PROP_NAME);
    }

    /**
     * Method to create a signature for a message using the designated private key
     * @param message the message to be signed with the private key
     * @return the signed message as a String; null if Error
     */
    @Override
    public byte[] sign (final byte[] message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get PrivateKey object from File
            PrivateKey privateKey = getPrivateKeyFromFile();
            signature.initSign(privateKey);
            signature.update(message);
            return signature.sign();
        } catch (Exception e) {
            System.err.println("Error while signing: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to verify a message with a signature, given a message, the signature and the sender name
     * (takes the public key from the corresponding entry in the communication list)
     * @param message the received signed message (only text without the signature)
     * @param receivedSignature the received signature as String
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     * @throws IllegalArgumentException if sender null or does not exist, or no Signature Key for sender
     */
    @Override
    public boolean verify (final byte[] message, final byte[] receivedSignature,
                           final String sender) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get public key of sender from the db
            Contact senderEntry = QuantumnetworkControllcenter.communicationList.query(sender);
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
            signature.update(message);
            // return result of verification
            return signature.verify(receivedSignature);
        } catch (Exception e) {
            System.err.println("Error while verifying: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method to generate a PublicKey object from a matching String
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
     * Method to get the private key from the {@link #privateKeyFile} in the SignatureKeys folder
     * @return a PrivateKey object created from the key in the file, null if error
     */
    private PrivateKey getPrivateKeyFromFile () {
        try {
            String currentPath = Configuration.getBaseDirPath();
            System.out.println(currentPath + KEY_PATH + privateKeyFile);
            if(!Files.exists(Path.of(currentPath + KEY_PATH + privateKeyFile))) {
                System.err.println("Error while creating a private key from the signature key file: "
                        + "no signature key file found");
                return null;
            }
            String keyString = readKeyStringFromFile(privateKeyFile);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
            return kf.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            System.err.println("Error while creating a private key from the signature key file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to read a key from the specified file in the SignatureKeys folder
     * and return it as a string
     * (expects the file name extension to be included in the parameter,
     * accepts the ones included in {@link #KEY_FILENAME_SYNTAX} )
     * @param fileName the name of the key file
     * @return the key from the file as a string (without the beginning and end lines like "-----BEGIN-----"), null if error
     */
    public static String readKeyStringFromFile(String fileName) {
        try {
            if(!Pattern.matches(KEY_FILENAME_SYNTAX, fileName)) {
                System.err.println("Error while creating a key string from the input file: "
                        + "wrong key file format");
                return null;
            }
            String currentPath = Configuration.getBaseDirPath();
            String key = new String (Files.readAllBytes
                    (Path.of(currentPath + KEY_PATH + fileName)));
            return key
                    .replaceAll("-----.{5,50}-----", "")
                    .replace(System.lineSeparator(), "");
        } catch (Exception e) {
            System.err.println("Error while creating a key string from the input file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to delete the key pair currently set as default key file
     * @return true if the deleting worked or already no default file set, false if error
     */
    public static boolean deleteSignatureKeys() {
        try {
            boolean res1 = deleteSignatureKey(privateKeyFile);
            if (res1) {
                setPrivateKey("");
            }
            boolean res2 = deleteSignatureKey(publicKeyFile);
            if(res2) {
                setPublicKey("");
            }
            return (res1 && res2);
        } catch (Exception e) {
            System.err.println("Problem deleting the current signature keys: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method to delete the key file with the given file
     * @param keyFileName the name of the key file to be deleted
     * @return true if the deleting worked or the file didn't exist, false if error
     */
    public static boolean deleteSignatureKey(String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        try {
            if(!keyFileName.equals("") &&
                    Files.exists(Path.of(currentPath + KEY_PATH + keyFileName))) {
                Files.delete(Path.of(currentPath + KEY_PATH + keyFileName));
            }
            return true;
        } catch (Exception e) {
            System.err.println("Problem deleting the signature key: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method to set the private key file to be used in {@link #sign(byte[])}
     * @param keyFileName the name of the key file to set as standard private key
     *                    including the file name extension;
     *                    accepts "" (an empty string) as input for setting it to no key
     * @return true if it worked, false otherwise
     */
    public static boolean setPrivateKey (String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        if (keyFileName == null) {
            privateKeyFile = "";
        } else if (keyFileName.equals("")
                || Files.exists(Path.of(currentPath + KEY_PATH + keyFileName))) {
            privateKeyFile = keyFileName;
        } else {
            System.err.println("Error while setting the private key: "
                    + "File does not exist.");
            return false;
        }
        Configuration.setProperty(PRIVATE_KEY_PROP_NAME, privateKeyFile);
        return true;
    }

    /**
     * Method to set the public key to be used by the communication partner in {@link #verify(byte[], byte[], String)}
     * @param keyFileName the name of the key file to set as standard public key
     *                    including the file name extension
     *                    accepts "" (an empty string) as input for setting it to no key
     * @return true if it worked, false otherwise
     */
    public static boolean setPublicKey (String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        if (keyFileName == null) {
            publicKeyFile = "";
        } else if (keyFileName.equals("")
                || Files.exists(Path.of(currentPath + KEY_PATH + keyFileName))) {
            publicKeyFile = keyFileName;
        } else {
            System.err.println("Error while setting the public key: "
                    + "File does not exist.");
            return false;
        }
        Configuration.setProperty(PUBLIC_KEY_PROP_NAME, publicKeyFile);
        return true;
    }

    /**
     * Generates a key pair for signing messages
     * (calls the other generateSignatureKeyPair Method with default parameters)
     * uses the default file name, specified by the class
     * deletes the key files if there are any with the same name (the default file name)
     * sets the created Key Pair as new standard keys
     * deletes the currently set standard keys (even if they don't have the default file name)
     * @return true if it worked, false if error
     */
    public static boolean generateSignatureKeyPair () {
        return generateSignatureKeyPair(DEFAULT_KEY_FILE_NAME, true, true);
    }

    /**
     * Generates a key pair for signing messages, using the chosen name for the key files
     * (public key as .pub file, private key as .key file)
     * @param keyFileName name for the created Key Pair
     * @param setAsKeyFile if true, sets the created Key Pair as new standard keys,
     *                     using {@link #setPrivateKey(String)} and {@link #setPublicKey(String)}
     * @param overwrite if true, any existing file with the same name will be overwritten
     * @return true if it worked, false if error
     */
    public static boolean generateSignatureKeyPair (String keyFileName, boolean setAsKeyFile, boolean overwrite) {
        try {
            String currentPath = Configuration.getBaseDirPath();
            // delete keys with the same name as the new ones if they exist and overwrite is true
            if(overwrite) {
                deleteSignatureKey(keyFileName + ".key");
                deleteSignatureKey(keyFileName + ".pub");
            } else if (Files.exists(Path.of(currentPath + KEY_PATH + keyFileName + ".key")) ||
                    Files.exists(Path.of(currentPath + KEY_PATH + keyFileName + ".pub"))) {
                System.err.println("Error while creating a key pair: "
                        + "File name exists already, but should not be overwritten. "
                        + "No new key created.");
                return false;
            }
            // generate the new keys
            KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
            kpGenerator.initialize(2048);
            KeyPair keyPair = kpGenerator.generateKeyPair();
            PublicKey pub = keyPair.getPublic();
            PrivateKey pvt = keyPair.getPrivate();
            Files.write(Path.of(currentPath + KEY_PATH + keyFileName + ".key"),
                    Base64.getEncoder().encode(pvt.getEncoded()));
            Files.write(Path.of(currentPath + KEY_PATH + keyFileName + ".pub"),
                    Base64.getEncoder().encode(pub.getEncoded()));
            // set as new standard keys if setAsKeyFile is true
            if (setAsKeyFile) {
                setPrivateKey(keyFileName + ".key");
                setPublicKey(keyFileName + ".pub");
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating a key pair: " + e.getMessage());
            return false;
        }
    }
}
