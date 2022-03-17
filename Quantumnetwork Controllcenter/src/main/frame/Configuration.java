package frame;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Class to save configurations and other values
 * Config file will be at the same place as the jar file with the program
 * @author Sarah Schumann
 */
public class Configuration {

    /**
     * The path to the folder for the program data, includes a file separator at the end
     * This is where the communication list, received files, etc. will be stored
     */
    private static String basePath;

    /**
     * The local path, where the jar file is located
     * This is also where config file is expected to be located
     */
    private static final String LOCAL_PATH = System.getProperty("user.dir") + File.separator;

    /**
     * The key name for the entry in the config file
     */
    private static final String PATH_CONFIG_NAME = "basePathConfig";

    /**
     * Name of the base directory at the basePath location
     * This directory will contain the sub-directories (e.g. SignatureKeys)
     */
    private static final String BASE_DIR = "QNCC" + File.separator;

    /**
     * Name of the config file
     * expected to be located in the path {@link #LOCAL_PATH}
     */
    private static final String CONFIG_FILE_NAME = "config.xml";

    /**
     * The list of the needed directories for the program
     * Will be created at the location described by the {@link #BASE_DIR}
     */
    private static final String[] DIRECTORY_LIST =
            {"SignatureKeys", "python", "connections", "externalAPI", "logs"};

    /**
     * Utility method to check whether the properties file is at the expected place
     * Creates an empty properties file if not existent
     * Should be always used at the start of the program
     * @return true if the file exist, false if not and thus newly created
     * @throws IOException if error while reading the existing config file
     */
    public static boolean findProperties () throws IOException {
        if(Files.exists(Path.of(LOCAL_PATH + CONFIG_FILE_NAME))) {
            try {
                // create an input stream
                InputStream in = Files.newInputStream(Path.of(LOCAL_PATH + CONFIG_FILE_NAME));
                // read the properties from file
                Properties properties = new Properties();
                properties.loadFromXML(in);
                in.close();
                return true;
            } catch (IOException e) {
                throw new IOException("Error while reading the config file at " + LOCAL_PATH
                        + "(" + e + ")");
            }
        } else {
            createConfigFile();
            basePath = LOCAL_PATH;
            setProperty(PATH_CONFIG_NAME, basePath);
            return false;
        }
    }

    /**
     * Gets the path at which the "base directory" is located,
     * this directory is the parent directory for the communication list,
     * the key store, the signature keys folder etc.
     * @return the base path as String
     */
    public static String getBaseDirPath() {
        if (basePath == null) {
            basePath = getProperty(PATH_CONFIG_NAME);
            if (basePath == null) basePath = ""; // if getProperty returns null (happens on first launch)
        }
        return basePath + BASE_DIR;
    }

    /**
     * Method to change the base path, where all the folders and other data should be placed
     * Calls the createFolders method for creating the folders in the new location
     * @param absolutePath the new base path as a String of the absolute path
     * @return true if it worked, false if not or error
     */
    public static boolean changeBasePath (final String absolutePath) {
        if (!Files.exists(Path.of(absolutePath))) {
            System.err.println("Error while changing the base path: directory/file does not exist.");
            return false;
        } else if (!Files.isDirectory(Path.of(absolutePath))) {
            System.err.println("Error while changing the base path: path is not a directory.");
            return false;
        } else {
            if (absolutePath.endsWith(File.separator)) {
                basePath = absolutePath;
            } else {
                basePath = absolutePath + File.separator;
            }
            createFolders();
            return setProperty(PATH_CONFIG_NAME, basePath);
        }
    }

    /**
     * Creates all the program folders listed in the class variable,
     * if they do not exist already
     * @return true if it worked or the folders existed already, false if error
     */
    public static boolean createFolders () {
        try {
            getBaseDirPath();
            if (!Files.exists(Path.of(basePath + BASE_DIR))) {
                Files.createDirectory(Path.of(basePath + BASE_DIR));
            }
            for (String dir : DIRECTORY_LIST) {
                if (!Files.exists(Path.of(basePath + BASE_DIR + dir))) {
                    Files.createDirectory(Path.of(basePath + BASE_DIR + dir));
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating the program folders: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method to create a new properties file if non exists
     * @return true if it worked or already there, false if error
     */
    private static boolean createConfigFile() {
        try {
            if (Files.exists(Path.of(LOCAL_PATH + CONFIG_FILE_NAME))) {
                return true;
            }
            OutputStream out = Files.newOutputStream(Path.of(LOCAL_PATH + CONFIG_FILE_NAME));
            Properties properties = new Properties();
            properties.storeToXML(out, null, StandardCharsets.ISO_8859_1);
            out.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating a properties file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Method to get the property value for the given property key
     * @param propertyKey the key for the desired value as String
     * @return the property for the key as String,
     *         null if not there or error
     */
    public static String getProperty (final String propertyKey) {
        try {
            // create an input stream
            InputStream in = Files.newInputStream(Path.of(LOCAL_PATH + CONFIG_FILE_NAME));
            // read the properties from file
            Properties properties = new Properties();
            properties.loadFromXML(in);
            in.close();
            return properties.getProperty(propertyKey);
        } catch (Exception e) {
            System.err.println("Error while reading or returning a property: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to set a property key-value pair
     * Overwrites any previous values for this key, if there were any
     * Creates a new config file, if non existed
     * @param propertyKey the key for the property as String
     * @param propertyValue the value for the property as String
     * @return true if it worked, false if error
     */
    public static boolean setProperty (String propertyKey, String propertyValue) {
        try {
            Properties properties = new Properties();
            if (Files.exists(Path.of(LOCAL_PATH + CONFIG_FILE_NAME))) {
                // create an input stream
                InputStream in = Files.newInputStream(Path.of(LOCAL_PATH + CONFIG_FILE_NAME));
                // read the properties from file
                properties.loadFromXML(in);
                in.close();
            } else {
                createConfigFile();
            }
            properties.setProperty(propertyKey, propertyValue);
            // create an output stream
            OutputStream out = Files.newOutputStream(Path.of(LOCAL_PATH + CONFIG_FILE_NAME));
            properties.storeToXML(out, null, StandardCharsets.ISO_8859_1);
            out.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error while setting the property \"" + propertyKey + "\" to the value \"" + propertyValue + "\" " + e.getMessage());
            return false;
        }
    }
}
