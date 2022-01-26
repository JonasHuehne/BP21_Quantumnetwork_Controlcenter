package KeyStore;

import org.junit.jupiter.api.Test;

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
    void doesKSIdExistTest(){
        boolean existing = KeyStoreDbManager.doesKeyStreamIdExist("nurEineTestID_01");
        assertEquals(true, existing);

        boolean nonExisting = KeyStoreDbManager.doesKeyStreamIdExist("nichtVorhandeneID");
        assertEquals(false, nonExisting);
    }

    @Test
    void deleteEntryByID() {
        boolean deleteBool = KeyStoreDbManager.deleteKeyInformationByID("nurEineTestID_01");

        assertEquals(true, deleteBool);

    }

    @Test
    void failedDeletion(){
        boolean failedDeletionBool = KeyStoreDbManager.deleteKeyInformationByID("nichtVorhandeneID");
        assertEquals(false, failedDeletionBool);
    }


    @Test
    void changeUsedTest(){
        boolean changeBool = KeyStoreDbManager.changeKeyToUsed("nurEineTestID_02");
    }

    @Test
    void getEntryTest() {
        KeyStoreObject testObject = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_02");
        assert testObject != null;
        byte[] testBuffer = testObject.getKeyBuffer();
        int testIndex = testObject.getIndex();
        System.out.println(new String(testBuffer));


        assertEquals("66778899", new String(testBuffer) );
        assertEquals(2, testIndex);

    }

    @Test
    void failedGetEntry(){
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("gibtsNicht");
        assertNull(obj);
    }

    @Test
    void getEntrysAsListTest(){
        byte[] keyBufferArray = "123456789".getBytes();

        // Bei jedem weiteren Durchlauf (nach dem 1.) muss die Zeile kommentiert werden da es nicht zweimal den selben eintrag in der Datenbank geben darf!
        KeyStoreDbManager.insertToKeyStore("NewEntryID", keyBufferArray, 1, "nirgendwo", "TuDarmstadt", false);
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreAsList();

        byte[] newEntryBuffer = testList.get(1).getKeyBuffer(); // only 2 Entrys in DB
        assertEquals(new String(keyBufferArray), new String(newEntryBuffer));
    }

    @Test
    void addKeyBufferTest(){
        boolean insertBool = KeyStoreDbManager.insertToKeyStore("KSID_Ohne_Key", null, 3, "München","Darmstadt", false );
        assertEquals(true, insertBool);

        byte[] keyTobeInserted = "0110101".getBytes();

        boolean addKeyBool = KeyStoreDbManager.addKeyBuffer("KSID_Ohne_Key", keyTobeInserted);
        assertEquals(true, addKeyBool);

        byte[] neuerKey = KeyStoreDbManager.getEntryFromKeyStore("KSID_Ohne_Key").getKeyBuffer();
        assertEquals(new String(keyTobeInserted), new String(neuerKey));

    }

    @Test
    void failedAddKeyBufferTest(){
        byte[] testKey = "28736432".getBytes();
        boolean failedKeyAddition = KeyStoreDbManager.addKeyBuffer("KSID_Ohne_Key", testKey);
        assertEquals(false, failedKeyAddition);
    }









}