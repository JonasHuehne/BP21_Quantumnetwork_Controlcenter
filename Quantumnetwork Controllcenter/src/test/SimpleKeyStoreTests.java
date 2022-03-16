import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;
import keyStore.SimpleKeyStore;

/**
 * Tests for the {@linkplain SimpleKeyStore} class.
 * @author Sasha Petri
 */
public class SimpleKeyStoreTests {

	static Random r;
	
	@BeforeAll
	public static void init() {
		r = new Random();
	}
	
	@Test
	public void can_connect() throws SQLException {
		SimpleKeyStore.connect();
	}
	
	@Test
	public void can_create_table_if_not_exists() throws SQLException {
		SimpleKeyStore.createTableIfNotExists();
	}
	
	@Test
	public void can_insert_and_retrieve() throws SQLException, NotEnoughKeyLeftException, NoKeyForContactException {
		// random contact name
		String contactName = "testContact" + r.nextInt();
		
		// random key for test contact
		final int keyLength = 16;
		byte[] randomKey = new byte[keyLength];
		(new Random()).nextBytes(randomKey);	
		
		// insert key
		SimpleKeyStore.insertEntry(contactName, randomKey, false);
		
		// attempt to retrieve it
		byte[] retrievedKey = SimpleKeyStore.getKeyBytes(contactName, keyLength);
		
		// stored key should be equal to generated key
		assertArrayEquals(randomKey, retrievedKey);
	}
	
	@Test
	public void can_retrieve_first_n_bytes() throws SQLException, NotEnoughKeyLeftException, NoKeyForContactException {
		// random contact name
		String contactName = "testContact" + r.nextInt();
		
		// random key for test contact
		final int keyLength = 16;
		byte[] randomKey = new byte[keyLength];
		(new Random()).nextBytes(randomKey);	
		
		// insert key
		SimpleKeyStore.insertEntry(contactName, randomKey, false);
		
		// attempt to retrieve the first 8 bytes (actually the "next" 8 bytes, but index is 0 so it's the first)
		byte[] retrievedKey = SimpleKeyStore.getKeyBytes(contactName, 8);
		
		// retrieved array should be the first 8 bytes of the inserted key
		byte[] randomKeySubArray = Arrays.copyOf(randomKey, 8);
		assertArrayEquals(randomKeySubArray, retrievedKey);
	}
	
	@Test
	public void increment_adjusts_remaining_bytes_correctly() throws SQLException, NoKeyForContactException {
		// random contact name
		String contactName = "testContact" + r.nextInt();
				
		// random key for test contact
		final int keyLength = 16;
		byte[] randomKey = new byte[keyLength];
		(new Random()).nextBytes(randomKey);	
				
		// insert key
		SimpleKeyStore.insertEntry(contactName, randomKey, false);
		
		// increment index by 4
		SimpleKeyStore.incrementIndex(contactName, 4);
		
		// remaining bytes should now be 12
		assertEquals(12, SimpleKeyStore.getRemainingBytes(contactName));
	}
	
	public void increment_adjust_returned_key_correctly() throws SQLException, NotEnoughKeyLeftException, NoKeyForContactException {
		// random contact name
		String contactName = "testContact" + r.nextInt();
				
		// random key for test contact
		final int keyLength = 16;
		byte[] randomKey = new byte[keyLength];
		(new Random()).nextBytes(randomKey);	
				
		// insert key
		SimpleKeyStore.insertEntry(contactName, randomKey, false);
		
		// increment index by 4
		SimpleKeyStore.incrementIndex(contactName, 4);

		// retrieve the next 8 bytes (key[4] to key[12])
		byte[] subKey = SimpleKeyStore.getKeyBytes(contactName, 8);
		
		// assert that they are equal to that sub-array of the key
		byte[] randomKeySubArray = Arrays.copyOfRange(randomKey, 4, 13);
		assertArrayEquals(randomKeySubArray, subKey);
	}
	
	public void deleting_used_key_bytes_functions_as_intended() throws SQLException, NotEnoughKeyLeftException, NoKeyForContactException {
		// random contact name
		String contactName = "testContact" + r.nextInt();
				
		// random key for test contact
		final int keyLength = 16;
		byte[] randomKey = new byte[keyLength];
		(new Random()).nextBytes(randomKey);	
				
		// insert key
		SimpleKeyStore.insertEntry(contactName, randomKey, false);
		
		// increment index by 4
		SimpleKeyStore.incrementIndex(contactName, 4);

		// delete the "used" bytes
		SimpleKeyStore.deleteUsedKeyBytes(contactName);
		
		// now there should be 12 bytes remaining
		assertEquals(12, SimpleKeyStore.getRemainingBytes(contactName));
		
		// make sure they are the correct bytes (last 12 bytes of initially inserted array)
		byte[] remainingKeyBytes = SimpleKeyStore.getKeyBytes(contactName, 12);
		byte[] expectedRemainingKeyBytes = Arrays.copyOfRange(randomKey, 4, 16);
		assertArrayEquals(expectedRemainingKeyBytes, remainingKeyBytes);
	}
}
