package messengerSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import communicationList.Contact;
import exceptions.NoValidPublicKeyException;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import graphicalUserInterface.GenericWarningMessage;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;
import graphicalUserInterface.CESignatureQueryDialog;
import networkConnection.ConnectionEndpoint;

import javax.swing.*;

/**
 * Class providing the methods necessary for authentication
 * using SHA256 with RSA 2048
 * @author Sarah Schumann
 */
public class SHA256withRSAAuthentication implements SignatureAuthentication {

    /**
     * Flags for the verification process
     */
    public static boolean continueVerify;
    public static boolean abortVerify;
    public static boolean discardMessage;

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
    private String privateKeyFile = "";

    /**
     * Name of the field for the public key file name in the properties file
     */
    private static final String PUBLIC_KEY_PROP_NAME = "publicKeyFile";
    /**
     * File name of the currently used own public key file
     * should include the file name extension
     */
    private String publicKeyFile = "";

    /**
     * Logger for error handling
     */
    private static Log log = new Log(SHA256withRSAAuthentication.class.getName(), LogSensitivity.WARNING);
    
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
     * @return the signed message as a byte array; null if Error
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
        }  catch (InvalidKeyException e){
        	log.logWarning("An invalid key was used", e);
        	return null;
    	}
        catch (Exception e) {
            log.logError("Error while signing", e);
            return null;
        }
    }

    /**
     * Method to verify a message with a signature, given a message, the signature and the sender name
     * (takes the public key from the corresponding entry in the communication list)
     * @param message the received signed message (without the signature)
     * @param receivedSignature the received signature
     * @param sender the sender of the message, needed to look up the public key in the communication list
     * @return true if the signature matches the message, false otherwise or if Error
     * @throws IllegalArgumentException if sender null or does not exist, or no Signature Key for sender
     */
    @Override
    public boolean verify (final byte[] message, final byte[] receivedSignature,
                           final String sender) {
        String pubKeyString;
        Contact senderEntry = QuantumnetworkControllcenter.communicationList.query(sender);
        if(senderEntry == null
                || senderEntry.getSignatureKey().equals(Utils.NO_KEY)
                || senderEntry.getSignatureKey() == null) {
            ConnectionEndpoint senderCE = QuantumnetworkControllcenter.conMan.getConnectionEndpoint(sender);
            pubKeyString = senderCE.getSigKey();
            if (pubKeyString.equals("")) {
                continueVerify = false;
                abortVerify = false;
                boolean invalidKey = true;
                new CESignatureQueryDialog(sender);
                while (invalidKey) {
                    if (abortVerify) {
                        log.logWarning("Verification aborted, message will be shown unauthenticated.", new NoValidPublicKeyException(sender));
                        return true;
                    } else if (discardMessage) {
                        log.logError("Verification aborted, message will be discarded.", new NoValidPublicKeyException(sender));
                        return false;
                    } else if (continueVerify) {
                        PublicKey publicKey = getPublicKeyFromString(senderCE.getSigKey());
                        if (publicKey == null) {
                            new CESignatureQueryDialog(sender);
                            GenericWarningMessage noKeyWarning = new GenericWarningMessage("Invalid public key entered.");
                            noKeyWarning.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                            noKeyWarning.setAlwaysOnTop(true);
                            continueVerify = false;
                        } else {
                            pubKeyString = senderCE.getSigKey();
                            invalidKey = false;
                        }
                    }
                }
            }
        } else {
            pubKeyString = senderEntry.getSignatureKey();
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            // get PublicKey object from String
            PublicKey publicKey = getPublicKeyFromString(pubKeyString);
            signature.initVerify(publicKey);
            signature.update(message);
            // return result of verification
            return signature.verify(receivedSignature);
        } catch (InvalidKeyException e){
        	log.logWarning("An invalid key was used", e);
        	return false;
    	} catch (Exception e) {
            log.logError("Error while verifying", e);
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
            log.logError("Error while creating a public key from the input string", e);
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
            log.logInfo("Getting private key from Path:" + currentPath + Utils.KEY_PATH + privateKeyFile);
            if(!Files.exists(Path.of(currentPath + Utils.KEY_PATH + privateKeyFile))) {
                log.logWarning("Error while creating a private key from the signature key file: "
                        + "no signature key file found at Path: " + currentPath + Utils.KEY_PATH + privateKeyFile);
                return null;
            }
            String keyString = Utils.readKeyStringFromFile(privateKeyFile);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyString));
            return kf.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            log.logError("Error while creating a private key from the signature key file at Path: " + Configuration.getBaseDirPath() + Utils.KEY_PATH + privateKeyFile, e);
            return null;
        }
    }

    /**
     * Method to delete the key pair currently set as default key file
     * @return true if the deleting worked or already no default file set, false if error
     */
    @Override
    public boolean deleteSignatureKeys() {
        boolean res1 = deleteSignatureKey(privateKeyFile);
        if (res1) {
            setPrivateKey("");
        }
        boolean res2 = deleteSignatureKey(publicKeyFile);
        if (res2) {
            setPublicKey("");
        }
        if (res1 && res2) {
            return true;
        } else {
            log.logWarning("Problem deleting the current signature key");
            return false;
        }
    }

    /**
     * Method to delete the key file with the given file
     * @param keyFileName the name of the key file to be deleted
     * @return true if the deleting worked or the file didn't exist, false if error
     */
    @Override
    public boolean deleteSignatureKey(String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        try {
            if(!keyFileName.equals("") &&
                    Files.exists(Path.of(currentPath + Utils.KEY_PATH + keyFileName))) {
                Files.delete(Path.of(currentPath + Utils.KEY_PATH + keyFileName));
            }
            return true;
        } catch (Exception e) {
            log.logError("Problem deleting the signature key", e);
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
    @Override
    public boolean setPrivateKey (String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        if (keyFileName == null || keyFileName.equals("")) {
            privateKeyFile = Utils.NO_KEY;
        } else if (Files.exists(Path.of(currentPath + Utils.KEY_PATH + keyFileName))) {
            privateKeyFile = keyFileName;
        } else {
            log.logWarning("Error while setting the private key: "
                    + "File does not exist.");
            return false;
        }
        return Configuration.setProperty(PRIVATE_KEY_PROP_NAME, privateKeyFile);
    }

    /**
     * Method to set the public key to be used by the communication partner in {@link #verify(byte[], byte[], String)}
     * @param keyFileName the name of the key file to set as standard public key
     *                    including the file name extension
     *                    accepts "" (an empty string) as input for setting it to no key
     * @return true if it worked, false otherwise
     */
    @Override
    public boolean setPublicKey (String keyFileName) {
        String currentPath = Configuration.getBaseDirPath();
        if (keyFileName == null) {
            publicKeyFile = "";
        } else if (keyFileName.equals("")
                || Files.exists(Path.of(currentPath + Utils.KEY_PATH + keyFileName))) {
            publicKeyFile = keyFileName;
        } else {
            log.logWarning("Error while setting the public key: "
                    + "File does not exist.");
            return false;
        }
        Configuration.setProperty(PUBLIC_KEY_PROP_NAME, publicKeyFile);
        return true;
    }

    /**
     * Method to check if a valid key pair is currently set
     * @return true if there currently is a valid key pair set, false if not or error
     */
    @Override
    public boolean existsValidKeyPair() {
        String currentPath = Configuration.getBaseDirPath();
        if(privateKeyFile.equals("") || publicKeyFile.equals("")) {
            return false;
        } else if (!Files.exists(Path.of(currentPath + Utils.KEY_PATH + privateKeyFile))
                || !Files.exists(Path.of(currentPath + Utils.KEY_PATH + publicKeyFile))) {
            return false;
        } else {
            try {
                Signature signature = Signature.getInstance("SHA256withRSA");
                PrivateKey privateKey = getPrivateKeyFromFile();
                PublicKey publicKey = getPublicKeyFromString(Utils.readKeyStringFromFile(publicKeyFile));
                signature.initSign(privateKey);
                byte[] message = MessageSystem.stringToByteArray("Hello");
                signature.update(message);
                byte[] sig = signature.sign();
                signature.initVerify(publicKey);
                signature.update(message);
                return signature.verify(sig);
            } catch (Exception e) {
                System.err.println("Error: No valid Key Pair set: " + e);
                return false;
            }
        }
    }

    /**
     * Generates a key pair for signing messages
     * (calls the other generateSignatureKeyPair Method with default parameters)
     * uses the default file name, specified by the class
     * deletes the key files if there are any with the same name (the default file name)
     * sets the created Key Pair as new own standard keys
     * deletes the currently set standard keys (even if they don't have the default file name)
     * @return true if it worked, false if error
     */
    @Override
    public boolean generateSignatureKeyPair () {
        return generateSignatureKeyPair(DEFAULT_KEY_FILE_NAME, true, true, true);
    }

    /**
     * Generates a key pair for signing messages, using the chosen name for the key files
     * (public key as .pub file, private key as .key file)
     * @param keyFileName name for the created Key Pair
     * @param setAsKeyFile if true, sets the created Key Pair as new own standard keys,
     *                     using {@link #setPrivateKey(String)} and {@link #setPublicKey(String)}
     * @param deleteCurrent if true, deletes the currently set standard keys
     * @param overwrite if true, any existing file with the same name will be overwritten
     * @return true if it worked, false if error
     */
    @Override
    public boolean generateSignatureKeyPair (String keyFileName, boolean setAsKeyFile,
                                                    boolean deleteCurrent, boolean overwrite) {
        try {
            String currentPath = Configuration.getBaseDirPath();
            // delete current standard keys if deleteCurrent is true
            if(deleteCurrent) {
                deleteSignatureKeys();
            }
            // delete keys with the same name as the new ones if they exist and overwrite is true
            if(overwrite) {
                deleteSignatureKey(keyFileName + ".key");
                deleteSignatureKey(keyFileName + ".pub");
            } else if (Files.exists(Path.of(currentPath + Utils.KEY_PATH + keyFileName + ".key")) ||
                    Files.exists(Path.of(currentPath + Utils.KEY_PATH + keyFileName + ".pub"))) {
                log.logWarning("Error while creating a key pair: "
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
            Files.write(Path.of(currentPath + Utils.KEY_PATH + keyFileName + ".key"),
                    Base64.getEncoder().encode(pvt.getEncoded()));
            Files.write(Path.of(currentPath + Utils.KEY_PATH + keyFileName + ".pub"),
                    Base64.getEncoder().encode(pub.getEncoded()));
            // set as new standard keys if setAsKeyFile is true
            if (setAsKeyFile) {
                setPrivateKey(keyFileName + ".key");
                setPublicKey(keyFileName + ".pub");
            }
            return true;
        } catch (Exception e) {
            log.logError("Error while creating a key pair", e);
            return false;
        }
    }
}