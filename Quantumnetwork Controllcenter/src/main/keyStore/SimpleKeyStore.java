package keyStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import exceptions.NoKeyForContactException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;

/**
 * This class supplies methods for creating, editing, getting and deleting entries from the KeyStore.db which holds all the keys
 * @implNote Originally, we attempted to comply with ETSI GS QKD 004 in this paper,
 * however, inexperience and organizational mistakes led to that attempt being messy.
 * We have opted to prioritize a well-structured project over compatibility in this case,
 * and leave adjusting this program to fit the ETSI key delivery standards for the future.
 * @author Sasha Petri, Aron Hernandez
 *
 */
public class SimpleKeyStore {
	
	/*
	 * To keep things simple, we have (for now) restricted
	 * ourselves to getting key segments byte wise.
	 * This may be a problem if a symmetric encryption scheme is used
	 * where the key size is not a multiple of 8, but an irregular
	 * key size like that is very much an edge case.
	 */

	/** Name of the database that the keys are stored in */
	private static final String dataBaseName = "KeyStore_2.db";
	/** Name of the table that the keys are stored in */
	private static final String tableName = "KeyStorage";
	
	/** Opens the connection to the Database, if it is not open yet 
	 * @throws SQLException if an error occured trying to connect to the database */
	public static Connection connect() throws SQLException { // make this private after initial testing
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) { // only occurs if the class is missing, i.e. compilation error
			System.err.println("Could not find the class used for database management (org.sqlite.JDBC).");
			e.printStackTrace();
			return null;
		} 
		String currentPath = Configuration.getBaseDirPath();
		return DriverManager.getConnection("jdbc:sqlite:" + currentPath + dataBaseName);
	}
	
	/**
	 * Creates the table used for storing keys if it does not exist.
	 * @throws SQLException 
	 * 		if an error occurred connecting to the database
	 */
	public static void createTableIfNotExists() throws SQLException {
		try (Connection connection = connect()) {
			Statement stmt = connection.createStatement();
	        String sql = "CREATE TABLE IF NOT EXISTS " + tableName
	                + "(ContactName VARCHAR UNIQUE, "
	                + "Key BLOB NOT NULL, "
	                + "KeyIndex INTEGER, "
	                + "Initiative BOOLEAN, " 
	                + "KeyLength INTEGER, " // not 100% needed, but useful for quicker queries, memory complexity
	                + "PRIMARY KEY (ContactName));";	
	        stmt.executeUpdate(sql);
		}
        return;
	}
	
	/**
	 * Inserts a new entry into the key storage.
	 * @param name
	 * 		name of the contact this is a key for <br>
	 * 		must be unique (one key per contact)
	 * @param key
	 * 		the key to save for that contact
	 * @param iniative
	 * 		true if you initiated the key generation, false otherwise <br>
	 * 		used to determine priority when key bits are used
	 * @throws SQLException
	 * 		if there was an error connecting to the database, or if the insert failed
	 */
	public static void insertEntry(String name, byte[] key, boolean iniative) throws SQLException {
		createTableIfNotExists();
		try (Connection connection = connect()) {
			String sql = "INSERT INTO " + tableName + "(ContactName, Key, KeyIndex, Initiative, KeyLength) VALUES(?, ?, ?, ?, ?)";
	        PreparedStatement stmt = connection.prepareStatement(sql);
	        stmt.setString(1, name);
	        stmt.setBytes(2, key);
	        stmt.setInt(3, 0);
	        stmt.setBoolean(4, iniative);
	        stmt.setInt(5, key.length);
	        stmt.executeUpdate();
		}
	}
	
	/**
	 * Gets the next n bytes of the key for the given contact.
	 * Does not automatically increment the index.
	 * @param contactName
	 * 		the contact to get the key bytes from
	 * @param nbytes
	 * 		the amount of bytes to get <br>
	 * 		must be greater than 0
	 * @return
	 * 		the next {@code nbytes} bytes of the contacts key
	 * @throws SQLException 
	 * 		if there was an error connecting to the database or querying it (e.g. table does not exist)
	 * @throws NotEnoughKeyLeftException 
	 * @throws NoKeyForContactException 
	 */
	public static byte[] getKeyBytes(String contactName, int nbytes) throws SQLException, NotEnoughKeyLeftException, NoKeyForContactException {
		if (nbytes <= 0) throw new IllegalArgumentException("Requested byte amount must be at least 1.");
		try (Connection connection = connect()) {
			String queryString = "SELECT Key, KeyIndex FROM " + tableName + " WHERE ContactName = ?";
			PreparedStatement queryStatement = connection.prepareStatement(queryString);
			queryStatement.setString(1, contactName);
			ResultSet rs = queryStatement.executeQuery();	
			if (!rs.isBeforeFirst()) { // if result set is empty
				throw new NoKeyForContactException("No key for contact " + contactName + " in the database.");
			} else {
				int 	currentIndex = rs.getInt(2);
				byte[]	key			 = rs.getBytes(1);
				// check that there are enough bytes left
				if (key.length - currentIndex < nbytes) {
					throw new NotEnoughKeyLeftException(contactName, nbytes, key.length - currentIndex);
				} else {
					// isolate the first n bytes as the return array
					byte[] outBytes = Arrays.copyOfRange(key, currentIndex, currentIndex + nbytes);
					// return the requested bytes
					return outBytes;
				}
			}
		}
	}
	
	/**
	 * Increments the key index, marking more of the key bytes as used.
	 * @param contactName
	 * 		the contact to increment the key index for
	 * @param bytes
	 * 		how much to increment the index / how many bytes are to be marked as used <br>
	 * 		if this exceeds the amount of remaining usable key bytes, all key bytes are marked as used
	 * @throws SQLException 
	 * 		if an error occured connecting to the database, or while querying or updating it
	 * @throws NoKeyForContactException 
	 * 		if the contact specified by {@code contactName} has no key in the database
	 */
	public static void incrementIndex(String contactName, int bytes) throws SQLException, NoKeyForContactException {
		try (Connection connection = connect()) {
			// Get current Index for contact
			String queryStmtStr = "SELECT KeyIndex, KeyLength FROM " + tableName + " WHERE ContactName = ?";
			PreparedStatement queryStmt = connection.prepareStatement(queryStmtStr);
			queryStmt.setString(1, contactName);
			ResultSet rs = queryStmt.executeQuery();
			
			if (!rs.isBeforeFirst()) { // rs empty ==> no such contact in DB
				throw new NoKeyForContactException("No key for contact " + contactName + " in the database. Could not increment index.");
			} else {
				// Increment index to at most keyLength 
				int index = rs.getInt(1);
				int keyLengthMax = rs.getInt(2);
				int newIndex = Math.min(keyLengthMax, index + bytes);
				// Update
				String updateStmtStr = "UPDATE " + tableName + " SET KeyIndex = ? WHERE ContactName = ? ";
				PreparedStatement updateStmt = connection.prepareStatement(updateStmtStr);
				updateStmt.setInt(1, newIndex);
				updateStmt.setString(2, contactName);
				updateStmt.executeUpdate();
			}
		}
	}
	
	/**
	 * Gets how many bytes of key are left for a contact.
	 * @param contactName
	 * 		the contact to check
	 * @return
	 * 		how many bytes of unused key are left for this contact <br>
	 * 		0 if no such contact is in the DB
	 * @throws SQLException 
	 * 		if an error occurred trying to connect to the DB or while querying it
	 */
	public static int getRemainingBytes(String contactName) throws SQLException {
		try (Connection connection = connect()) {
			String queryStmtStr = "SELECT KeyIndex, KeyLength FROM " + tableName + " WHERE ContactName = ?";
			PreparedStatement queryStmt = connection.prepareStatement(queryStmtStr);
			queryStmt.setString(1, contactName);
			ResultSet rs = queryStmt.executeQuery();
			// due to UNIQUE constraint on name, rs may have at most one entry
			if (!rs.isBeforeFirst()) { // if rs is empty
				return 0;
			} else {
				return rs.getInt(2) - rs.getInt(1); // key length - index = remaining bytes
			}
		}
	}
	
	/**
	 * Deletes the used key bytes for the given contact.
	 * @param contactName
	 * 		the contact whose used key bytes are to be removed from the DB
	 * @throws SQLException 
	 * @throws NoKeyForContactException 
	 */
	public static void deleteUsedKeyBytes(String contactName) throws SQLException, NoKeyForContactException {
		try (Connection connection = connect()) {
			String queryString = "SELECT Key, KeyIndex FROM " + tableName + " WHERE ContactName = ?";
			PreparedStatement queryStatement = connection.prepareStatement(queryString);
			queryStatement.setString(1, contactName);
			ResultSet rs = queryStatement.executeQuery();
			if (!rs.isBeforeFirst()) { //if rs is empty
			 	throw new NoKeyForContactException("No key for contact " + contactName + " in the database.");
			} else { // if entry for contact is found
				// create a new key array without the first #index bytes, because index is how many bytes were used
				byte[] key = rs.getBytes(1);
				int index = rs.getInt(2);
				byte[] newKey = Arrays.copyOfRange(key, index, key.length); 
				// update the key entry to be the new key, also update Key Length and set Index to 0 (no bytes of remaining key are used yet)
				String updateString = "UPDATE " + tableName + " SET Key = ? , KeyIndex = ? , KeyLength = ? WHERE ContactName = ? ";
				PreparedStatement updateStatement = connection.prepareStatement(updateString);
				updateStatement.setBytes(1, newKey); // new key
				updateStatement.setInt(2, 0); // new index
				updateStatement.setInt(3, newKey.length); // new key length
				updateStatement.setString(4, contactName);
				updateStatement.executeUpdate();
			}
		}
	}
	
	/**
	 * Appends the passed bytes to the key stored for the given contact.
	 * @param contactName
	 * 		the contact to add additional key bytes for
	 * @param key
	 * 		the key bytes to add
	 */
	public static void appendKeyBytes(String contactName, byte[] key) {
		System.err.println("Not implemented yet.");
	}
	
	/**
	 * Deletes the entry for the specified contact if it exists.
	 * @param contactName
	 * 		the contact whose entry is to be deleted
	 * @throws SQLException 
	 * 		if there was an error connecting to the database, or an error while deleting the entry
	 */
	public static void deleteEntryIfExists(String contactName) throws SQLException {
		try (Connection connection = connect()) {
			String deleteString = "DELETE FROM " + tableName + " WHERE ContactName = ?";
			PreparedStatement deleteStatement = connection.prepareStatement(deleteString);
			deleteStatement.setString(1, contactName);
			deleteStatement.executeUpdate();
		}
	}
	
}
