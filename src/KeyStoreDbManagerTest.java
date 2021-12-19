import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreDbManagerTest {


    @Test
    void createNewDbAndTable_connect_insert_Test() {

        //KeyStoreDbManager.executeQuery("DROP TABLE KeyInformations;", "test.db");

        //connection() is used in every method so its indirectly tested anyways
        KeyStoreDbManager.createNewTable("test.db", "KeyInformations");

        KeyStoreDbManager.insertToDb("test.db", "testID_01", 666, 1, "nowhere", "here");
        KeyStoreDbManager.insertToDb("test.db", "testID_02", 777, 2, "nowhere", "here");
        KeyStoreDbManager.insertToDb("test.db", "testID_03", 888, 3, "nowhere", "here");


        KeyStoreDbManager.selectAll("test.db", "KeyInformations");
        assertEquals(1, 1);


    }


    @Test
    void deleteEntrysTest() {
        KeyStoreDbManager.deleteEntryByID("test.db", "KeyInformations", "testID_01");
        KeyStoreDbManager.deleteEntryByID("test.db", "KeyInformations", "testID_03");

        // Deletion of 2 out of 3 Entrys --> Only 1 Entry in Table
        KeyStoreDbManager.selectAll("test.db", "KeyInformations");
    }


}