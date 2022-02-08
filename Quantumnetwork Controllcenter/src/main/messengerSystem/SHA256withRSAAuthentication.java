package messengerSystem;

import communicationList.Contact;
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
 * class providing the methods necessary for authentication
 * using SHA256 with RSA 2048
 * @author Sarah Schumann
 */
public class SHA256withRSAAuthentication implements Authentication {

    private static final String KEY_PATH = System.getProperty("user.dir")
            + File.separator + "SignatureKeys" + File.separator;

    private static final String PROPERTIES_PATH = System.getProperty("user.dir")
            + File.separator + "properties" + File.separator;

    private static final String STRINGS_FILE_NAME = "strings.xml";

    private static final String DEFAULT_KEY_FILE_NAME = "signature";

    private static final String PRIVATE_KEY_PROP_NAME = "privateKeyFile";
    private static String PRIVATE_KEY_FILE = "";

    private static final String PUBLIC_KEY_PROP_NAME = "publicKeyFile";
    private static String PUBLIC_KEY_FILE = "";

    // accepted file name extensions are:
    // .pub .pem .key .der .txt or no extension
    private static final String KEY_FILE_SYNTAX =
            "(.+\\.pub)|(.+\\.pem)|(.+\\.key)|(.+\\.der)|(.+\\.txt)|(^[^.]+$)";

    /**
     * Constructor of the class, calls the methods to check
     * if the needed folders and files exist
     */
    public SHA256withRSAAuthentication() {
        checkSignatureFolderExists();
        Properties properties = getStringProperties();
        setPrivateKey(properties.getProperty(PRIVATE_KEY_PROP_NAME), false);
        setPublicKey(properties.getProperty(PUBLIC_KEY_PROP_NAME), false);
    }

    /**
     * a method to check, whether the folder for the signature keys already exists
     * creates it, if not
     * @return true if already there or created, false if error
     */
    private static boolean checkSignatureFolderExists() {
        try {
            if (!Files.exists(Path.of(KEY_PATH))) {
                Files.createDirectory(Path.of(KEY_PATH));
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating the SignatureKeys directory: " + e.getMessage());
            return false;
        }
    }

    /**
     * a method to check, whether the folder for the properties already exists,
     * as well as if the xml file for the strings is there or not;
     * sets the properties for the key files to the current status
     * if it was newly created (using {@link #setKeyProperties(Properties)})
     * @return true if already there or created successfully, false if error
     */
    private static boolean checkPropertiesExist () {
        try {
            if (!Files.exists(Path.of(PROPERTIES_PATH))) {
                Files.createDirectory(Path.of(PROPERTIES_PATH));
            }
            if (!Files.exists(Path.of(PROPERTIES_PATH + STRINGS_FILE_NAME))) {
                // create the file by using the method with null parameter
                return setKeyProperties(null);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating the properties directory or strings.xml file: " + e.getMessage());
            return false;
        }
    }

    /**
     * a method to get the current key properties,
     * will check if the file exists by calling {@link #checkPropertiesExist()}
     * @return the properties as a Properties object, null if error
     */
    private static Properties getStringProperties () {
        try {
            checkPropertiesExist();
            // create an input stream
            FileInputStream in = new FileInputStream(PROPERTIES_PATH + STRINGS_FILE_NAME);
            // read the properties from file
            Properties properties = new Properties();
            properties.loadFromXML(in);
            in.close();
            return properties;
        } catch (Exception e) {
            System.err.println("Error while reading the properties: " + e.getMessage());
            return null;
        }
    }

    /**
     * a method to set the Key Properties to the current status
     * @param prop the current properties in the file,
     *             !can be set to null, if nothing in the file yet
     * @return true if it worked, false if error
     */
    private static boolean setKeyProperties (Properties prop) {
        try {
            // create an output stream
            FileOutputStream out = new FileOutputStream(PROPERTIES_PATH + STRINGS_FILE_NAME);
            // set the properties
            Properties properties;
            if (prop == null) {
                properties = new Properties();
            } else {
                properties = prop;
            }
            properties.setProperty(PRIVATE_KEY_PROP_NAME, PRIVATE_KEY_FILE);
            properties.setProperty(PUBLIC_KEY_PROP_NAME, PUBLIC_KEY_FILE);
            // write the properties to the file
            properties.storeToXML(out, null, StandardCharsets.ISO_8859_1);
            out.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error while writing the properties: " + e.getMessage());
            return false;
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
     * method to get the private key from the set private key file in the SignatureKeys folder
     * @return a PrivateKey object created from the key in the file, null if error
     */
    private PrivateKey getPrivateKeyFromFile () {
        try {
            checkSignatureFolderExists();
            if(!Files.exists(Path.of(KEY_PATH + PRIVATE_KEY_FILE))) {
                System.err.println("Error while creating a private key from the signature key file: "
                        + "no signature key file found");
                return null;
            }
            String keyString = readKeyStringFromFile(PRIVATE_KEY_FILE);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
            return kf.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            System.err.println("Error while creating a private key from the signature key file: " + e.getMessage());
            return null;
        }
    }

    /**
     * method to read a key from the specified file in the SignatureKeys folder
     * and return it as a string
     * (expects the file name extension to be included in the parameter)
     * @param fileName the name of the key file
     * @return the key from the file as a string (without the beginning and end lines like "-----BEGIN-----"), null if error
     */
    public static String readKeyStringFromFile(String fileName) {
        try {
            if(!Pattern.matches(KEY_FILE_SYNTAX, fileName)) {
                System.err.println("Error while creating a key string from the input file: "
                        + "wrong key file format");
                return null;
            }
            checkSignatureFolderExists();
            String key = new String (Files.readAllBytes
                    (Path.of(KEY_PATH + fileName)));
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
            boolean res1 = deleteSignatureKey(PRIVATE_KEY_FILE);
            if (res1) {
                setPrivateKey("", true);
            }
            boolean res2 = deleteSignatureKey(PUBLIC_KEY_FILE);
            if(res2) {
                setPublicKey("", true);
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
        try {
            if(!keyFileName.equals("") &&
                    Files.exists(Path.of(KEY_PATH + keyFileName))) {
                Files.delete(Path.of(KEY_PATH + keyFileName));
            }
            return true;
        } catch (Exception e) {
            System.err.println("Problem deleting the signature key: " + e.getMessage());
            return false;
        }
    }

    /**
     * method to set a standard private key
     * @param keyFileName the name of the key file to set as standard private key
     *                    including the file name extension
     * @return true if it worked, false otherwise
     */
    public static boolean setPrivateKey (String keyFileName, boolean writeToProperties) {
        if (keyFileName.equals("")
                || Files.exists(Path.of(KEY_PATH + keyFileName))) {
            PRIVATE_KEY_FILE = keyFileName;
            if (writeToProperties) {
                return setKeyProperties(getStringProperties());
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * method to set a standard public key
     * @param keyFileName the name of the key file to set as standard public key
     *                    including the file name extension
     * @return true if it worked, false otherwise
     */
    public static boolean setPublicKey (String keyFileName, boolean writeToProperties) {
        if (keyFileName.equals("")
                || Files.exists(Path.of(KEY_PATH + keyFileName))) {
            PUBLIC_KEY_FILE = keyFileName;
            if (writeToProperties) {
                return setKeyProperties(getStringProperties());
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * generates a key pair for signing messages, using the default file name
     * deletes the key files if there are any with the same name
     * sets the created Key Pair as new standard keys
     * deletes the current standard keys
     * deletes any key files with the default file name
     * @return true if it worked, false if error
     */
    public static boolean generateSignatureKeyPair () {
        return generateSignatureKeyPair(DEFAULT_KEY_FILE_NAME, true, true, true);
    }

    /**
     * generates a key pair for signing messages with the chosen file name
     * (public key with .pub, private key with .key)
     * deletes the key files if there are any with the same name
     * @param keyFileName name for the created Key Pair
     * @param setAsKeyFile if true, sets the created Key Pair as new standard keys
     * @param deleteCurrent if true, deletes the currently set standard keys
     * @param overwrite if true, any existing file with the same name will be overwritten
     * @return true if it worked, false if error
     */
    public static boolean generateSignatureKeyPair (String keyFileName, boolean setAsKeyFile,
                                                    boolean deleteCurrent, boolean overwrite) {
        try {
            checkSignatureFolderExists();
            // delete current standard keys if deleteCurrent is true
            if(deleteCurrent) {
                deleteSignatureKeys();
            }
            // delete keys with the same name as the new ones if they exist and overwrite is true
            if(overwrite) {
                deleteSignatureKey(keyFileName + ".key");
                deleteSignatureKey(keyFileName + ".pub");
            } else if (Files.exists(Path.of(KEY_PATH + keyFileName + ".key")) ||
                    Files.exists(Path.of(KEY_PATH + keyFileName + ".pub"))) {
                System.err.println("Error while creating a key pair: "
                        + "file name exists already, but should not be overwritten. "
                        + "No new key created");
                return false;
            }
            // generate the new keys
            KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA");
            kpGenerator.initialize(2048);
            KeyPair keyPair = kpGenerator.generateKeyPair();
            PublicKey pub = keyPair.getPublic();
            PrivateKey pvt = keyPair.getPrivate();
            Files.write(Path.of(KEY_PATH + keyFileName + ".key"),
                    Base64.getEncoder().encode(pvt.getEncoded()));
            Files.write(Path.of(KEY_PATH + keyFileName + ".pub"),
                    Base64.getEncoder().encode(pub.getEncoded()));
            // set as new standard keys if setAsKeyFile is true
            if (setAsKeyFile) {
                setPrivateKey(keyFileName + ".key", true);
                setPublicKey(keyFileName + ".pub", true);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating a key pair: " + e.getMessage());
            return false;
        }
    }
}
