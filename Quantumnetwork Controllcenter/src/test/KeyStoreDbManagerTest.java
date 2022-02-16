import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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

        boolean insertBool1 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", firstArray, "vonHier", "nachHier", false);
        boolean insertBool2 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_02", secondArray, "vonHier", "nachHier", false);
        boolean insertBool3 = KeyStoreDbManager.insertToKeyStore("nurEineTestID_03", thirdArray, "vonHier", "nachDort", false);
        assertEquals(true, insertBool1);
        assertEquals(true, insertBool2);
        assertTrue(insertBool3);
        KeyStoreObject testObj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_01");
        assertEquals(0, testObj.getIndex());
    }

    @Test
    void failedInsertion(){
        //KeyStreamID ist bereits in DB
        boolean failedInsertion = KeyStoreDbManager.insertToKeyStore("nurEineTestID_01", "987364".getBytes(), "100.187.878", "764937", false);
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
    void getCorrectRangeOfKey(){

        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        byte[] correctKey = obj.getKeyFromStartingIndex(64);

        assertEquals(64, correctKey.length);
    }

    @Test
    void passiveIndexUpdateTest(){
        //previous test should have updated the index automatically
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        assertEquals(96, obj.getIndex());
    }

    @Test
    void failedGetKeyFromStartingIndexTest() {
        // this test should return a byte[] of length 32 instead of 64 because there is not enough keyMaterial left
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
        byte[] correctKey = obj.getKeyFromStartingIndex(64);
        //error message should be displayed
        assertEquals(32, obj.getKeyFromStartingIndex(64).length);
    }


    @Test
    void changeIndexWithNotEnoughMaterialLeft(){

        // there is not enough keymaterial left to add 128 bit to the index as it is already at 96 (byte[] of length 128)
        boolean indexChange = KeyStoreDbManager.changeIndex("nurEineTestID_03", 128);
        assertFalse(indexChange);

        // check if Entry was updated to used
        KeyStoreObject obj = KeyStoreDbManager.getEntryFromKeyStore("nurEineTestID_03");
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
        KeyStoreDbManager.insertToKeyStore("NewEntryID", keyBufferArray, "nirgendwo", "TuDarmstadt", false);
        ArrayList<KeyStoreObject> testList = KeyStoreDbManager.getKeyStoreAsList();

        byte[] newEntryBuffer = testList.get(2).getKeyBuffer(); // only 3 Entrys in DB
        assertEquals(new String(keyBufferArray), new String(newEntryBuffer));
    }

    @Test
    void addKeyBufferTest(){
        boolean insertBool = KeyStoreDbManager.insertToKeyStore("KSID_Ohne_Key", null, "München","Darmstadt", false );
        assertEquals(true, insertBool);

        byte[] keyTobeInserted = "0110101".getBytes();

        boolean addKeyBool = KeyStoreDbManager.changeKeyBuffer("KSID_Ohne_Key", keyTobeInserted);
        assertEquals(true, addKeyBool);

        byte[] neuerKey = KeyStoreDbManager.getEntryFromKeyStore("KSID_Ohne_Key").getKeyBuffer();
        assertEquals(new String(keyTobeInserted), new String(neuerKey));

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