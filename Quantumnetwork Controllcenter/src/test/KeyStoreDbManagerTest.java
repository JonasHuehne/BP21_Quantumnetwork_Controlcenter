
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;
import keyStore.KeyStoreDbManager;
import keyStore.KeyStoreObject;

/**
 *  Class for testing the methods of KeyStoreDbManager
 *
 *  Tests have to be carried out one by one in order!
 *
 * @author Aron Hernandez, Sasha Petri
 */

class KeyStoreDbManagerTest {

    //Junit 5.7

	@BeforeAll
	static void destroyKeyStore() throws IOException {
		Files.deleteIfExists(Paths.get(Configuration.getBaseDirPath(), "KeyStore.db"));
	}
	
	@BeforeEach
	void clearKeyStore() throws SQLException {
		if (Files.exists(Paths.get(Configuration.getBaseDirPath(), "KeyStore.db"))) {
			for (KeyStoreObject o : KeyStoreDbManager.getKeyStoreAsList()) {
				KeyStoreDbManager.deleteEntryIfExists(o.getID());
			}
		}
		KeyStoreDbManager.createNewKeyStoreAndTable();
		assertTrue(Files.exists(Paths.get(Configuration.getBaseDirPath(), "KeyStore.db")));
	}

    @Test
    void insert_and_get_works() throws NoKeyWithThatIDException, SQLException {
    	KeyStoreDbManager.createNewKeyStoreAndTable();
        // 128 bits
        byte[] firstArray = "11111101000010001111111100101111100100101011100111111111001100000101110011101010101011100110011101011100111010001111101000101001".getBytes();
        byte[] secondArray = "11001100010100011010111011110000001011000011000000010100100011110010110100011111110001110001101111100011101010000110101111110111".getBytes();
        byte[] thirdArray = "10011110010000010001101100001001100000010101011110101010101011111111100010010110100110111010001110000010000111101001111111110100".getBytes();

        String id1 = "nurEineTestID_01";
        String id2 = "nurEineTestID_02";
        String id3 = "nurEineTestID_03";
        
        KeyStoreDbManager.insertToKeyStore(id1, firstArray, "vonHier", "nachHier", false, true);
        KeyStoreDbManager.insertToKeyStore(id2, secondArray, "vonHier", "nachHier", false, false);
        KeyStoreDbManager.insertToKeyStore(id3, thirdArray, "vonHier", "nachDort", false, false);
        
        // all entries inserted
        ArrayList<KeyStoreObject> entries = KeyStoreDbManager.getKeyStoreAsList();
        assertNotNull(entries);
        assertEquals(3, entries.size());
        
        // retrieving entries individually works
        KeyStoreObject e1 = KeyStoreDbManager.getEntryFromKeyStore(id1);
        KeyStoreObject e2 = KeyStoreDbManager.getEntryFromKeyStore(id2);
        KeyStoreObject e3 = KeyStoreDbManager.getEntryFromKeyStore(id3);
        
        assertNotNull(e1);
        assertNotNull(e2);
        assertNotNull(e3);
        
        // Entries data is set correctly
        
        assertEquals(id1, e1.getID());
        assertEquals(id2, e2.getID());
        assertEquals(id3, e3.getID());
        
        assertArrayEquals(firstArray, e1.getCompleteKeyBuffer());
        assertArrayEquals(secondArray, e2.getCompleteKeyBuffer());
        assertArrayEquals(thirdArray, e3.getCompleteKeyBuffer());
        
        assertEquals("vonHier", e1.getSource());
        assertEquals("nachHier", e1.getDestination());
        
        assertTrue(e1.getInitiative());
        assertFalse(e2.getInitiative());
        
        assertFalse(e1.isUsed());
        
    }

    @Test
    void failedInsertion(){
    	// Attempt to insert same data twice
    	assertThrows(SQLException.class, () -> {
        	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, true);
        	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, true);
    	});
    }
    
    @Test
    void changeIndexTest() throws NoKeyWithThatIDException, SQLException, NotEnoughKeyLeftException{
    	byte[] AliceKey = new byte[1024];
    	(new Random()).nextBytes(AliceKey);
    	KeyStoreDbManager.insertToKeyStore("Alice", AliceKey, "vonHier", "nachHier", false, true);
    	
    	// Changing index forwards works
    	KeyStoreDbManager.changeIndex("Alice", 100);
    	assertEquals(100, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	
    	// Changing index backwards works
    	KeyStoreDbManager.changeIndex("Alice", 50);
    	assertEquals(50, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	
    	// Changing index to what it is does nothing 
    	KeyStoreDbManager.changeIndex("Alice", 50);
    	assertEquals(50, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	
    	// Changing index to be > key length throws an exception
    	assertThrows(NotEnoughKeyLeftException.class, () -> KeyStoreDbManager.changeIndex("Alice", 9001));
    	
    	// Changing index of key that doesn't exist throws an exception
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.changeIndex("Charlie", 231));
    	
    }
    
    @Test
    void incrementIndexTest() throws SQLException, NoKeyWithThatIDException {
     	byte[] AliceKey = new byte[1024];
    	(new Random()).nextBytes(AliceKey);
    	KeyStoreDbManager.insertToKeyStore("Alice", AliceKey, "vonHier", "nachHier", false, true);

    	// Incrementing Index works
    	KeyStoreDbManager.incrementIndex("Alice", 100);
    	assertEquals(100, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	KeyStoreDbManager.incrementIndex("Alice", 40);
    	assertEquals(140, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	
    	// Incrementing index beyond max sets to max instead
    	KeyStoreDbManager.incrementIndex("Alice", 1337);
    	assertEquals(1024, KeyStoreDbManager.getEntryFromKeyStore("Alice").getIndex());
    	
    	// Can not increment index for key that doesn't exist
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.incrementIndex("Charles", 70));
    	
    	// Illegal arguments are illegal
    	assertThrows(IllegalArgumentException.class, () -> KeyStoreDbManager.incrementIndex("Alice", -1));
    	assertThrows(IllegalArgumentException.class, () -> KeyStoreDbManager.incrementIndex("Alice", 0));
    	
    }

    @Test
    void failedGetKeyTest() {
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.getEntryFromKeyStore("Axel"));
    }

    @Test
    void doesKSIdExistTest() throws SQLException{
    	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, true);
    	
        assertTrue(KeyStoreDbManager.doesKeyStreamIdExist("Alice"));
        assertFalse(KeyStoreDbManager.doesKeyStreamIdExist("Gunther"));
    }

    @Test
    void deleteEntryByID() throws SQLException {
    	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, true);
    	KeyStoreDbManager.deleteEntryIfExists("Alice");
    	
    	assertFalse(KeyStoreDbManager.doesKeyStreamIdExist("Alice"));
    }

    @Test
    void changeKeyBuffer() throws NoKeyWithThatIDException, SQLException{
     	byte[] oldKey = new byte[1024];
     	byte[] newKey = new byte[1024];
     	Random r = new Random();
     	r.nextBytes(oldKey);
     	r.nextBytes(newKey);
     	
     	KeyStoreDbManager.insertToKeyStore("Alice", oldKey, "vonHier", "nachHier", false, true);
     	
     	assertArrayEquals(oldKey, KeyStoreDbManager.getEntryFromKeyStore("Alice").getCompleteKeyBuffer());
     
     	KeyStoreDbManager.changeKeyBuffer("Alice", newKey);
     	
     	assertArrayEquals(newKey, KeyStoreDbManager.getEntryFromKeyStore("Alice").getCompleteKeyBuffer());

    }

    @Test
    void getInitiativeTest() throws NoKeyWithThatIDException, SQLException{
    	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, true);
    	KeyStoreDbManager.insertToKeyStore("Bob", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, false);
        KeyStoreObject alice = KeyStoreDbManager.getEntryFromKeyStore("Alice");
        KeyStoreObject bob = KeyStoreDbManager.getEntryFromKeyStore("Bob");
        
        assertTrue(alice.getInitiative());
        assertFalse(bob.getInitiative());
        
    }

    @Test
    void deleteUsedKeysTest() throws SQLException, NoKeyWithThatIDException{
     	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, false);
     	KeyStoreDbManager.insertToKeyStore("Bob", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, false);
     	KeyStoreDbManager.insertToKeyStore("Charles", new byte[] {1, 2, 3}, "vonHier", "nachHier", true, false);

    	KeyStoreDbManager.changeKeyToUsed("Alice");
    	
    	assertEquals(3, KeyStoreDbManager.getKeyStoreAsList().size());
    	
    	assertTrue(KeyStoreDbManager.deleteUsedKeys());
    	
    	assertEquals(1, KeyStoreDbManager.getKeyStoreAsList().size());
    	
    	assertTrue(KeyStoreDbManager.doesKeyStreamIdExist("Bob"));
    	assertFalse(KeyStoreDbManager.doesKeyStreamIdExist("Alice"));
    	assertFalse(KeyStoreDbManager.doesKeyStreamIdExist("Charles"));
    }

    @Test
    void failedDeleteUsedKeys() throws SQLException {
    	KeyStoreDbManager.insertToKeyStore("Alice", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, false);
     	KeyStoreDbManager.insertToKeyStore("Bob", new byte[] {1, 2, 3}, "vonHier", "nachHier", false, false);

     	assertFalse(KeyStoreDbManager.deleteUsedKeys());
     	
     	assertEquals(2, KeyStoreDbManager.getKeyStoreAsList().size());
       	assertTrue(KeyStoreDbManager.doesKeyStreamIdExist("Alice"));
       	assertTrue(KeyStoreDbManager.doesKeyStreamIdExist("Bob"));
    }

    @Test
    void can_not_change_non_existent_key() {
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.changeIndex("Max", 100));
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.incrementIndex("Max", 100));
    	assertThrows(NoKeyWithThatIDException.class, () -> KeyStoreDbManager.changeKeyToUsed("Max"));
    }

}