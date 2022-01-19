package KeyStore;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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
        byte[] firstArray = "1122334455".getBytes();
        byte[] secondArray = "66778899".getBytes();
        boolean insertBool1 = KeyStoreDbManager.insertKeyInformation("nurEineTestID_01", firstArray, 1, "vonHier", "nachHier");
        boolean insertBool2 = KeyStoreDbManager.insertKeyInformation("nurEineTestID_02", secondArray, 2, "vonHier", "nachHier");
        assertEquals(true, insertBool1);
        assertEquals(true, insertBool2);
    }

    @Test
    void deleteEntryByID() {
        boolean deleteBool = KeyStoreDbManager.deleteKeyInformationByID("nurEineTestID_01");


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
        KeyInformationObject testObject = KeyStoreDbManager.getEntryFromKeyInformation("komplettNeueID");
        assert testObject != null;
        byte[] testBuffer = testObject.getBuffer();
        int testIndex = testObject.getIndex();

        assertEquals(66778899, testBuffer);
        assertEquals(2, testIndex);

    }

    @Test
    void getEntrysAsListTest(){
        byte[] keyBufferArray = "1111111".getBytes();

        // Bei jedem weiteren Durchlauf (nach dem 1.) muss die Zeile kommentiert werden da es nicht zweimal den selben eintrag in der Datenbank geben darf!
        KeyStoreDbManager.insertKeyInformation("NewEntryID", keyBufferArray, 1, "nirgendwo", "TuDarmstadt");
        ArrayList<KeyInformationObject> testList = KeyStoreDbManager.getKeyInformationAsList();

        byte[] newEntryBuffer = testList.get(1).getBuffer(); // only 2 Entrys in DB
        assertEquals(keyBufferArray[1], newEntryBuffer[1]);
    }

    /**
     *  ------- Tests for the KeyStore Table -------
     */

    @Test
    void insertToKeyStoreTable(){
        KeyStoreDbManager manager = new KeyStoreDbManager();
        boolean bool1 = KeyStoreDbManager.insertToKeyStore("super sicherer Schlüssel", 0, "ersteID");
        boolean bool2 = KeyStoreDbManager.insertToKeyStore("2iuhfd92f7gsaao3gc<au", 0, "zweiteID");

        assertEquals(true, bool1);
        assertEquals(true, bool2);
    }

    @Test
    void getEntryFromKeyStoretest(){
        KeyStoreObject testObject = KeyStoreDbManager.getEntryFromKeyStore("ersteID");
        assertEquals("super sicherer Schlüssel", testObject.getKey());
        assertEquals(0, testObject.getUsed());
    }

    @Test
    void changeUsedStatusTest(){
        boolean bool1 = KeyStoreDbManager.changeKeyToUsed("ersteID");
        assertEquals(true, bool1);
        KeyStoreObject testObj = KeyStoreDbManager.getEntryFromKeyStore("ersteID");
        assertEquals(1, testObj.getUsed());
    }

    @Test
    void getAllEntriesfromKeyStoreTest(){
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreEntriesAsList();

        assertEquals("2iuhfd92f7gsaao3gc<au", testList.get(1).getKey());
        assertEquals("zweiteID", testList.get(1).getKeyStreamID());
    }

    @Test
    void deleteKeyStoreEntryTest(){
        boolean bool1 = KeyStoreDbManager.deleteKeyByID("zweiteID");
        boolean bool2 = KeyStoreDbManager.deleteKeyByID("ersteID");

        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreEntriesAsList();
        assertEquals(true, bool1);
        assertEquals(true, bool2);
        assertEquals(0, testList.size());
    }



}