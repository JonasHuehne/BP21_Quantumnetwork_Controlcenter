package messengerSystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import communicationList.Contact;
import frame.Configuration;
import frame.QuantumnetworkControllcenter;
import networkConnection.ConnectionEndpoint;

/**
 * Class for utility methods connected to this package
 * @author Sarah Schumann, Sasha Petri
 */
public class Utils {

    public static final String NO_KEY = "";

    /**
     * accepted file name extensions are:
     * .pub .pem .key .txt or no extension
     */
    private static final String KEY_FILENAME_SYNTAX =
            "(.+\\.pub)|(.+\\.pem)|(.+\\.key)|(.+\\.txt)|(^[^.]+$)";

    /**
     * The path to the folder for the signature keys, incl a file separator at the end
     */
    public static final String KEY_PATH = "SignatureKeys" + File.separator;

    /**
     * Method to read a key from the specified file in the SignatureKeys folder
     * and return it as a string
     * (expects the file name extension to be included in the parameter,
     * accepts the ones included in {@link #KEY_FILENAME_SYNTAX} )
     * @param fileName the name of the key file
     * @return the key from the file as a string (without the beginning and end lines like "-----BEGIN-----"), null if error
     */
    public static String readKeyStringFromFile(final String fileName) {
        try {
            if(!Pattern.matches(KEY_FILENAME_SYNTAX, fileName)) {
                System.err.println("Error while creating a key string from the input file: "
                        + "wrong key file format");
                return null;
            }
            final String currentPath = Configuration.getBaseDirPath();
            final String key = new String (Files.readAllBytes
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
     * Gets the public key for the specified contact.
     * 
     * It first searches the connection manager in the {@linkplain MessageSystem}
     * for any ConnectionEndpoint with the given name. If one is found,
     * and it has a public key entry that is not null and not empty,
     * that entry is returned.
     * 
     * If no such ConnectionEndpoint could be found, or if its entry was empty/null
     * then the CommunicationList currently in use by the QuantumnetworkControllcenter
     * is searched. If an entry with the given name exists, and it has a public key
     * that is not empty and not null, that public key is returned.
     * 
     * In any other case, null is returned.
     * 
     * @param contact
     * 		the contact to get a pk for
     * @return
     * 		a public key string as described above <br>
     * 		if only null or empty entries could be found, null is returned <br>
     * 		null is also returned if there is no CE or comm list entry for the given contact
     */
    public static String getPkIfPossible(String contact) {
    	ConnectionEndpoint ce = MessageSystem.conMan.getConnectionEndpoint(contact);
    	if (ce != null) {
    		String cePK = ce.getSigKey();
    		if (cePK != null) {
    			if (!cePK.isEmpty()) {
    				return cePK;
    			}
    		}
    	}
    	
    	Contact contactEntry = QuantumnetworkControllcenter.communicationList.query(contact);
    	if (contactEntry == null) {
    		return null;
    	} else {
    		String contactEntryPK = contactEntry.getSignatureKey();
    		if (contactEntryPK != null && !contactEntryPK.isEmpty()) {
    			return contactEntryPK;
    		}
    	}
    	
    	return null;
    }
    
}
