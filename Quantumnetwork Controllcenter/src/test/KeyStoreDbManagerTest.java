import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;

/**
 *  Class for testing the methods of KeyStoreDbManager
 *
 *  Tests have to be carried out one by one in order!
 *
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
        // 128 bits
        byte[] firstArray = "11111101000010001111111100101111100100101011100111111111001100000101110011101010101011100110011101011100111010001111101000101001".getBytes();
        byte[] secondArray = "11001100010100011010111011110000001011000011000000010100100011110010110100011111110001110001101111100011101010000110101111110111".getBytes();
        byte[] thirdArray = "10011110010000010001101100001001100000010101011110101010101011111111100010010110100110111010001110000010000111101001111111110100".getBytes();

        boolean insertBool1 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", firstArray, "vonHier", "nachHier", false, true);
        boolean insertBool2 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_02", secondArray, "vonHier", "nachHier", false, false);
        boolean insertBool3 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_03", thirdArray, "vonHier", "nachDort", false, false);
        assertEquals(true, insertBool1);
        assertEquals(true, insertBool2);
        assertTrue(insertBool3);
        KeyStoreObject testObj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_01");
        assertEquals(0, testObj.getIndex());
    }

    @Test
    void failedInsertion(){
        //KeyStreamID ist bereits in DB
        boolean failedInsertion = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", "987364".getBytes(), "100.187.878", "764937", false, true);
        assertEquals(false, failedInsertion);
    }

    @Test
    void changeIndexTest(){

        boolean indexChange = KeyStoreDbManager.changeIndex("nurEineTestID_03", 32);
        KeyStoreObject testObj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        int newIndex = testObj.getIndex();
        assertTrue(indexChange);
        assertEquals(32, newIndex);
    }

    @Test
    void getCorrectKeyPlusIndexChangeTest(){

        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        // method "getKey" updates the index automatically
        byte[] correctKey = obj.getKey(64);

        assertEquals(64, correctKey.length);
        //check if index was incremented instantly on object itself
        assertEquals(96, obj.getIndex());
    }

    @Test
    void passiveIndexUpdateTest(){
        //check if the index actually changed in the DB

        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        assertEquals(96, obj.getIndex());
    }

    @Test
    void failedGetKeyTest() {

        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        byte[] key = obj.getKey(64);
        //error message should be displayed

        // this test should return null as index is at 96 and a key of size 64 is being requested
        assertNull(key);
    }


    @Test
    void changeIndexWithNotEnoughMaterialLeft(){

        // there is not enough keymaterial left to add 265 bits (as byte[] is only of length 128)
        boolean indexChange = KeyStoreDbManager.changeIndex("nurEineTestID_02", 265);
        assertFalse(indexChange);

        // check if Entry was updated to used
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_02");
        assertEquals(true, obj.getUsed());

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
    void getEntryTest() {
        KeyStoreObject testObject = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_02");
        assert testObject != null;
        byte[] testBuffer = testObject.getCompleteKeyBuffer();
        int testIndex = testObject.getIndex();
        System.out.println(new String(testBuffer));


        assertEquals("11001100010100011010111011110000001011000011000000010100100011110010110100011111110001110001101111100011101010000110101111110111", new String(testBuffer) );
        assertEquals(0, testIndex);

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
        KeyStoreDbManager.insertToKeyStore("NewEntryID", keyBufferArray, "nirgendwo", "TuDarmstadt", false, true);
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreAsList();

        byte[] newEntryBuffer = testList.get(2).getCompleteKeyBuffer(); // only 3 Entrys in DB
        assertEquals(new String(keyBufferArray), new String(newEntryBuffer));
    }

    @Test
    void addKeyBufferTest(){
        boolean insertBool = KeyStoreDbManager.insertToKeyStore("KSID_Ohne_Key", null, "München","Darmstadt", false, false );
        assertEquals(true, insertBool);

        byte[] keyTobeInserted = "0110101".getBytes();

        boolean addKeyBool = KeyStoreDbManager.changeKeyBuffer("KSID_Ohne_Key", keyTobeInserted);
        assertEquals(true, addKeyBool);

        byte[] neuerKey = KeyStoreDbManager.getEntryFromKeyStore("KSID_Ohne_Key").getCompleteKeyBuffer();
        assertEquals(new String(keyTobeInserted), new String(neuerKey));

    }

    @Test
    void getInitiativeTest(){
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("NewEntryID");
        boolean initiative = obj.getInitiative();
        assertTrue(initiative);
    }

    @Test
    void deleteUsedKeysTest(){
        List<KeyStoreObject> beforeDeletionList = KeyStoreDbManager.getKeyStoreAsList();
        // There are 4 entries in the KeyStore
        assertEquals(4, beforeDeletionList.size());

        boolean deleteBool = KeyStoreDbManager.deleteUsedKeys();
        assertTrue(deleteBool);
        // 2 entries are used -> there should be 2 left
        List<KeyStoreObject> afterDeletionList = KeyStoreDbManager.getKeyStoreAsList();
        assertEquals(2, afterDeletionList.size());
    }

    @Test
    void failedDeleteUsedKeys(){
        //now there are only 2 (unused) entries in the KeyStore
        boolean failedDeletionBool = KeyStoreDbManager.deleteUsedKeys();
        // Error message should be displayed
        assertFalse(failedDeletionBool);
    }

    //TODO
    // unter welchen umständen soll ein keybuffer nicht geändert werden können?
    /**
     *

    @Test
    void failedAddKeyBufferTest(){
        byte[] testKey = "28736432".getBytes();
        boolean failedKeyAddition = KeyStoreDbManager.changeKeyBuffer("KSID_Ohne_Key", testKey);
        assertEquals(false, failedKeyAddition);
    }
    */








}