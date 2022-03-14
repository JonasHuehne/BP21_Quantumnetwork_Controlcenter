package messengerSystem;

import frame.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Class for utility methods connected to this package
 * @author Sarah Schumann
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
}
