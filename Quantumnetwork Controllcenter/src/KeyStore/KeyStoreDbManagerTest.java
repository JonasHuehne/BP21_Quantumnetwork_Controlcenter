package KeyStore;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  Class for testing the methods of KeyStoreDbManager
 * @author Aron Hernandez
 */

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
        boolean insertBool1 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", firstArray, 1, "vonHier", "nachHier", false);
        boolean insertBool2 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_02", secondArray, 2, "vonHier", "nachHier", false);
        assertEquals(true, insertBool1);
        assertEquals(true, insertBool2);
    }

    @Test
    void failedInsertion(){
        //KeyStreamID ist bereits in DB
        boolean failedInsertion = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", "987364".getBytes(), 3, "100.187.878", "764937", false);
        assertEquals(false, failedInsertion);
    }

    @Test
    void deleteEntryByID() {
        boolean deleteBool = KeyStoreDbManager.deleteKeyInformationByID("nurEineTestID_01");


        assertEquals(true, deleteBool);
        boolean selectBool = KeyStoreDbManager.selectAll();
        assertEquals(true, selectBool);
    }

    @Test
    void failedDeletion(){
        boolean failedDeletionBool = KeyStoreDbManager.deleteKeyInformationByID("nichtVorhandeneID");
        assertEquals(false, failedDeletionBool);
    }

    @Test
    void updateKeyStramIDTest() {
        boolean updateBool = KeyStoreDbManager.updateKeyStreamID(2, "komplettNeueID");
        assertEquals(true, updateBool);
        KeyStoreDbManager.selectAll();
    }

    @Test
    void changeUsedTest(){
        boolean changeBool = KeyStoreDbManager.changeKeyToUsed("komplettNeueID");
    }

    @Test
    void getEntryTest() {
        KeyStoreObject testObject = KeyStoreDbManager.getEntryFromKeyStore("komplettNeueID");
        assert testObject != null;
        byte[] testBuffer = testObject.getBuffer();
        int testIndex = testObject.getIndex();
        System.out.println(new String(testBuffer));


        assertEquals("66778899", new String(testBuffer) );
        assertEquals(2, testIndex);

    }

    @Test
    void getEntrysAsListTest(){
        byte[] keyBufferArray = "1111111".getBytes();

        // Bei jedem weiteren Durchlauf (nach dem 1.) muss die Zeile kommentiert werden da es nicht zweimal den selben eintrag in der Datenbank geben darf!
        KeyStoreDbManager.insertToKeyStore("NewEntryID", keyBufferArray, 1, "nirgendwo", "TuDarmstadt", false);
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreAsList();

        byte[] newEntryBuffer = testList.get(1).getBuffer(); // only 2 Entrys in DB
        assertEquals(keyBufferArray[1], newEntryBuffer[1]);
    }





}