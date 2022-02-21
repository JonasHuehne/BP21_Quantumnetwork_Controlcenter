package frame;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Configuration {

    /**
     * Name of the properties file
     */
    private static final String PROPERTIES_FILE_NAME = "properties.xml";

    /**
     * The path to the folder for the property files, incl a file separator at the end
     * Default path unless changed in the settings
     */
    private static String propertiesPath = System.getProperty("user.dir")
            + File.separator + "properties" + File.separator;

    /**
     *
     * @param absolutePath
     * @param moveProperties
     * @return
     */
    public static boolean changePath (String absolutePath, Boolean moveProperties) {
        // TODO
    }

    /**
     *
     * @return
     */
    public static boolean createProperties () {
        try {
            if (!Files.exists(Path.of(propertiesPath))) {
                Files.createDirectory(Path.of(propertiesPath));
            }
            // TODO
            return true;
        } catch (Exception e) {
            // TODO
            return false;
        }
    }

    /**
     *
     * @param propertyKey
     * @return
     */
    public static String getProperty (String propertyKey) {
        // TODO
    }

    /**
     *
     * @param propertyKey
     * @param propertyValue
     * @return
     */
    public static boolean setProperty (String propertyKey, String propertyValue) {
        // TODO
    }
}
