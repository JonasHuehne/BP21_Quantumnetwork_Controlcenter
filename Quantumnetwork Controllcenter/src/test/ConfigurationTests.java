import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import frame.Configuration;

/**
 * Class for testing the Configuration Class
 * IMPORTANT: The class will delete any previously existing config file
 * due to what needs to be tested.
 * The same holds for any folders in the test directories
 * @author Sarah Schumann
 */
public class ConfigurationTests {

    private static final String TEST_DIR_NAME = "testDirTest";
    private static final String TEST_DIR_NAME2 = "testDirTest2";
    // list of directories from the Configuration class; has to be adjusted to changes there
    private static final String[] DIRECTORY_LIST =
            {"SignatureKeys", "python", "connections", "externalAPI"};

    @BeforeAll
    static void backUpProperties() {
    	if(Files.exists(Path.of(System.getProperty("user.dir") + File.separator + "config.xml" ))) {
            try {
            	Files.copy(
            			Path.of(System.getProperty("user.dir") + File.separator + "config.xml" ),
            			Path.of(System.getProperty("user.dir") + File.separator + "config_backup.xml"));
            } catch (Exception e) {
                System.err.println("Error during test setup: " + e);
            }
        }
    }
    
    @AfterAll
    static void restoreProperties() {
    	if(Files.exists(Path.of(System.getProperty("user.dir") + File.separator + "config_backup.xml"))) {
            try {
            	Files.copy(
            			Path.of(System.getProperty("user.dir") + File.separator + "config_backup.xml"),
            			Path.of(System.getProperty("user.dir") + File.separator + "config.xml"));
            	Files.delete(Path.of(System.getProperty("user.dir") + File.separator + "config_backup.xml"));
            } catch (Exception e) {
                System.err.println("Error restoring the backup: " + e);
            }
        }
    }
    
    @BeforeEach
    @AfterEach
    void setupCleanup() {
        if(Files.exists(Path.of(System.getProperty("user.dir") + File.separator + "config.xml" ))) {
            try {
                Files.delete(Path.of(System.getProperty("user.dir") + File.separator + "config.xml"));
            } catch (Exception e) {
                System.err.println("Error during test setup: " + e);
            }
        }
    }

    @Test
    void testFindProperties() {
        // test 'normal' behavior
        try {
            boolean result1 = Configuration.findProperties();
            Assertions.assertFalse(result1);
            boolean result2 = Configuration.findProperties();
            Assertions.assertTrue(result2);
        } catch (IOException e) {
            System.err.println("Error during 'testFindProperties': " + e);
        }

        // setup for test handling of potentially corrupt file
        try {
            Files.delete(Path.of(System.getProperty("user.dir") + File.separator + "config.xml"));
            Files.createFile(Path.of(System.getProperty("user.dir") + File.separator + "config.xml"));
        } catch (Exception e) {
            System.err.println("Error during 'testFindProperties': " + e);
        }

        // test handling of potentially corrupt file
        Assertions.assertThrows(IOException.class, Configuration::findProperties);
    }

    @Nested
    class TestChangeBasePathAndCreateFolders {

        @BeforeEach
        void setup() {
            try {
                Configuration.findProperties();
                Files.createDirectory(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME));
                Files.createDirectory(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME2));
            } catch (Exception e) {
                System.err.println("Error during setup for 'TestChangeBasePathAndCreateFolders': " + e);
            }
        }

        @AfterEach
        void cleanup() {
            try {
                String currentBaseDirPath = System.getProperty("user.dir") + File.separator + TEST_DIR_NAME
                        + File.separator + "QNCC" + File.separator;
                for (String s : DIRECTORY_LIST) {
                    Files.deleteIfExists(Path.of(currentBaseDirPath + s));
                }
                Files.deleteIfExists(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME
                                + File.separator + "QNCC"));
                Files.deleteIfExists(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME));
                String currentBaseDirPath2 = System.getProperty("user.dir") + File.separator + TEST_DIR_NAME2
                        + File.separator + "QNCC" + File.separator;
                for (String s : DIRECTORY_LIST) {
                    Files.deleteIfExists(Path.of(currentBaseDirPath2 + s));
                }
                Files.deleteIfExists(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME2
                                + File.separator + "QNCC"));
                Files.deleteIfExists(Path.of
                        (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME2));
            } catch (Exception e) {
                System.err.println("Error during cleanup 'TestChangeBasePathAndCreateFolders': " + e);
            }
        }

        @Test
        void testChangeBasePath() {
            // test default path
            String result1 = Configuration.getBaseDirPath();
            Assertions.assertEquals(System.getProperty("user.dir") + File.separator
                            + "QNCC" + File.separator,
                    result1);

            // testing changing the base path (with and without given file separator at the end)
            boolean result2 = Configuration.changeBasePath
                    (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME);
            Assertions.assertTrue(result2);
            String result3 = Configuration.getBaseDirPath();
            Assertions.assertEquals
                    (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME
                            + File.separator + "QNCC" + File.separator, result3);
            boolean result4 = Configuration.changeBasePath
                    (System.getProperty("user.dir") + File.separator
                            + TEST_DIR_NAME2 + File.separator);
            Assertions.assertTrue(result4);
            String result5 = Configuration.getBaseDirPath();
            Assertions.assertEquals
                    (System.getProperty("user.dir") + File.separator + TEST_DIR_NAME2
                            + File.separator + "QNCC" + File.separator, result5);
        }

        @Test
        void testCreateFolders() {
            // change base path before checking folders
            if (!Configuration.changeBasePath(System.getProperty("user.dir") + File.separator
                    + TEST_DIR_NAME)) {
                System.err.println("Error while changing the path in testCreateFolders");
            }

            // check for created folders
            String currentBasePath = Configuration.getBaseDirPath();
            for (String s : DIRECTORY_LIST) {
                boolean result = Files.exists(Path.of(currentBasePath + s));
                Assertions.assertTrue(result);
            }

            // check for created folders after deleting one and then using the createFolders method
            try {
                Files.deleteIfExists(Path.of(currentBasePath + DIRECTORY_LIST[0]));
            } catch (Exception e) {
                System.err.println("Error during 'testCreateFolders': " + e);
            }
            boolean result6 = Configuration.createFolders();
            Assertions.assertTrue(result6);
            for (String s : DIRECTORY_LIST) {
                boolean result = Files.exists(Path.of(currentBasePath + s));
                Assertions.assertTrue(result);
            }
        }
    }

    @Nested
    class TestSetAndGetProperty {

        @BeforeEach
        void setup() {
            try {
                Configuration.findProperties();
            } catch (IOException e) {
                System.err.println("Error during setup for TestSetAndGetProperty: " + e);
            }
        }

        @Test
        void testCorrectSetAndGet() {
            String testProperty1 = "testProperty1";
            String testProperty2 = "testProperty2";

            boolean result1 = Configuration.setProperty(testProperty1, "testTestTest1");
            Assertions.assertTrue(result1);
            String result2 = Configuration.getProperty(testProperty1);
            Assertions.assertEquals("testTestTest1", result2);

            boolean result3 = Configuration.setProperty(testProperty2, "testTestTest2");
            Assertions.assertTrue(result3);
            String result4 = Configuration.getProperty(testProperty2);
            Assertions.assertEquals("testTestTest2", result4);

            boolean result5 = Configuration.setProperty(testProperty1, "testTestTest3");
            Assertions.assertTrue(result5);
            String result6 = Configuration.getProperty(testProperty1);
            Assertions.assertEquals("testTestTest3", result6);
            String result7 = Configuration.getProperty(testProperty2);
            Assertions.assertEquals("testTestTest2", result7);
        }

        @Test
        void testIncorrectGet() {
            String testProperty1 = "testProperty1";

            String result1 = Configuration.getProperty(testProperty1);
            Assertions.assertNull(result1);

            try {
                Files.deleteIfExists(Path.of(System.getProperty("user.dir") + File.separator
                        + "config.xml"));
            } catch (Exception e) {
                System.err.println("Error during 'testIncorrectGet': " + e);
            }

            String result2 = Configuration.getProperty(testProperty1);
            Assertions.assertNull(result2);
        }
    }
}
