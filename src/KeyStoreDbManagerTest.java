import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreDbManagerTest {

    //Junit 5.7

    @Test
    void createNewKeyStoreAndTable() {
        boolean bool1 = KeyStoreDbManager.createNewKeyStoreAndTable();
        assertEquals(true, bool1);
    }

    @Test
    void insertToDb() {
        boolean insertBool1 = KeyStoreDbManager.insertToDb("nurEineTestID_01", 1122334455, 1, "vonHier", "nachHier");
        boolean insertBool2 = KeyStoreDbManager.insertToDb("nurEineTestID_02", 66778899, 2, "vonHier", "nachHier");
        assertEquals(true, insertBool1);
        assertEquals(true, insertBool2);
    }

    @Test
    void deleteEntryByID() {
        boolean deleteBool = KeyStoreDbManager.deleteEntryByID("nurEineTestID_01");


        assertEquals(true, deleteBool);
        boolean selectBool = KeyStoreDbManager.selectAll();
        assertEquals(true, selectBool);
    }

    @Test
    void updateKeyStramIDTest() {
        boolean updateBool = KeyStoreDbManager.updateKeyStreamID(2, "komplettNeueID");
        assertEquals(true, updateBool);
        KeyStoreDbManager.selectAll();
    }

    @Test
    void getEntryTest() {
        KeyStoreObject testObject = KeyStoreDbManager.getEntry("komplettNeueID");
        assert testObject != null;
        int testBuffer = testObject.getBuffer();
        int testIndex = testObject.getIndex();

        assertEquals(66778899, testBuffer);
        assertEquals(2, testIndex);

    }

    @Test
    void getEntrysAsListTest(){

        // Bei jedem weiteren Durchlauf (nach dem 1.) muss die Zeile kommentiert werden da es nicht zweimal den selben eintrag in der Datenbank geben darf!
        KeyStoreDbManager.insertToDb("NewEntryID", 1111111, 1, "nirgendwo", "TuDarmstadt");
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getEntriesAsList();

        int newEntryBuffer = testList.get(1).getBuffer(); // only 2 Entrys in DB
        assertEquals(1111111, newEntryBuffer);
    }


}