package frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Configuration {

    /**
     * The path to the folder for the program data, incl a file separator at the end
     * Default path unless changed in the settings
     */
    private static String basePath = System.getProperty("user.dir") + File.separator;

    /**
     * Name of the properties file
     */
    private static final String PROPERTIES_FILE_NAME = "properties.xml";

    /**
     * The path to the folder for the property files starting from the base path,
     * incl a file separator at the end
     */
    private static final String PROPERTIES_PATH = "properties" + File.separator;

    private static final String PATH_FILE = ".basePath";

    /**
     *
     * @param absolutePath
     * @param moveProperties
     * @return
     */
    public static boolean changeBasePath (String absolutePath, Boolean moveProperties) {
        Properties properties;
        if (moveProperties) {
            properties = new Properties();
            try {
                // create an input stream
                FileInputStream in = new FileInputStream
                        (basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME);
                // read the properties from file
                properties.loadFromXML(in);
                in.close();
            } catch (Exception e) {
                System.err.println("Error while changing the base path: " + e.getMessage());
                return false;
            }
        } else {
            properties = null;
        }
        basePath = absolutePath;
        setPathInFile();
        return createProperties(properties);
    }

    /**
     *
     * @return
     */
    private static boolean setPathInFile () {
        try {
            Files.writeString(Path.of(System.getProperty("user.dir") + File.separator
                    + PATH_FILE), basePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error while setting the path in the .basePath file: " + e.getMessage());
            return false;
        }
    }

    /**
     *
     * @return
     */
    public static boolean findProperties () {
        if(Files.exists(Path.of(System.getProperty("user.dir") + File.separator
                + PATH_FILE))) {
            try {
                basePath = Files.readString(Path.of(System.getProperty("user.dir")
                        + File.separator + PATH_FILE));
                return Files.exists(Path.of(basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME));
            } catch (Exception e) {
                System.err.println("Error while trying to find the properties file: " + e.getMessage());
                return false;
            }
        } else {
            // TODO: workout whether this is necessary or what to do
            setPathInFile();
            return false;
        }
    }

    /**
     *
     * @return
     */
    public static boolean createProperties () {
        return createProperties(null);
    }

    /**
     *
     * @param prop
     * @return
     */
    private static boolean createProperties(Properties prop) {
        try {
            if (!Files.exists(Path.of(basePath + PROPERTIES_PATH))) {
                Files.createDirectory(Path.of(basePath + PROPERTIES_PATH));
            }
            if (!Files.exists(Path.of(basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME))) {
                // create an output stream
                FileOutputStream out = new FileOutputStream
                        (basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME);
                // set the properties
                Properties properties;
                if (prop == null) {
                     properties = new Properties();
                } else {
                    properties = prop;
                }
                properties.storeToXML(out, null, StandardCharsets.ISO_8859_1);
                out.close();
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error while creating a properties file: " + e.getMessage());
            return false;
        }
    }

    /**
     *
     * @param propertyKey
     * @return
     */
    public static String getProperty (String propertyKey) {
        if (Files.exists(Path.of(basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME))) {
            try {
                // create an input stream
                FileInputStream in = new FileInputStream
                        (basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME);
                // read the properties from file
                Properties properties = new Properties();
                properties.loadFromXML(in);
                in.close();
                return properties.getProperty(propertyKey);
            } catch (Exception e) {
                System.err.println("Error while reading or returning a property: " + e.getMessage());
                return null;
            }
        } else {
            createProperties();
            return null;
        }
    }

    /**
     *
     * @param propertyKey
     * @param propertyValue
     * @return
     */
    public static boolean setProperty (String propertyKey, String propertyValue) {
        try {
            Properties properties = new Properties();
            if (Files.exists(Path.of(basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME))) {
                // create an input stream
                FileInputStream in = new FileInputStream
                        (basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME);
                // read the properties from file
                properties.loadFromXML(in);
                in.close();
            } else {
                createProperties();
            }
            properties.setProperty(propertyKey, propertyValue);
            // create an output stream
            FileOutputStream out = new FileOutputStream
                    (basePath + PROPERTIES_PATH + PROPERTIES_FILE_NAME);
            properties.storeToXML(out, null, StandardCharsets.ISO_8859_1);
            out.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error while setting a property: " + e.getMessage());
            return false;
        }
    }
}
