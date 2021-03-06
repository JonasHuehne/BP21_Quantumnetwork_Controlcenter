package keyStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;
import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**
 * This class supplies methods for creating, editing, getting and deleting entries from a KeyStore.db which holds all the keys
 * @author Sasha Petri
 * @implNote Alternative to the {@linkplain KeyStoreDbManager}. Currently not in use.
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
	
	private static Log log = new Log(SimpleKeyStore.class.getName(), LogSensitivity.WARNING);
	
	/** Opens the connection to the Database, if it is not open yet 
	 * @throws SQLException if an error occurred trying to connect to the database */
	public static Connection connect() throws SQLException { // make this private after initial testing
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) { // only occurs if the class is missing, i.e. compilation error
			log.logError("Could not find the class used for database management (org.sqlite.JDBC).", e);
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
	                + "(KeyID VARCHAR UNIQUE, "
	                + "Key BLOB NOT NULL, "
	                + "KeyIndex INTEGER, "
	                + "Initiative BOOLEAN, " 
	                + "KeyLength INTEGER, " // not 100% needed, but useful for quicker queries, memory complexity
	                + "PRIMARY KEY (KeyID));";	
	        stmt.executeUpdate(sql);
		}
	}
	
	/**
	 * Inserts a new entry into the key storage.
	 * @param keyID
	 * 		unique ID under which this key is to be saved
	 * @param key
	 * 		the key to save
	 * @param initiative
	 * 		true if you initiated the key generation, false otherwise <br>
	 * 		used to determine priority when key bits are used
	 * @throws SQLException
	 * 		if there was an error connecting to the database, or if the insert failed
	 */
	public static void insertEntry(String keyID, byte[] key, boolean initiative) throws SQLException {
		createTableIfNotExists();
		try (Connection connection = connect()) {
			String sql = "INSERT INTO " + tableName + "(KeyID, Key, KeyIndex, Initiative, KeyLength) VALUES(?, ?, ?, ?, ?)";
	        PreparedStatement stmt = connection.prepareStatement(sql);
	        stmt.setString(1, keyID);
	        stmt.setBytes(2, key);
	        stmt.setInt(3, 0);
	        stmt.setBoolean(4, initiative);
	        stmt.setInt(5, key.length);
	        stmt.executeUpdate();
		}
	}
	
	/**
	 * Gets the next n bytes of the key for the given contact.
	 * Does not automatically increment the index.
	 * @param keyID
	 * 		the key to get the bytes from
	 * @param nbytes
	 * 		the amount of bytes to get <br>
	 * 		must be greater than 0
	 * @return
	 * 		the next {@code nbytes} bytes of the specified key
	 * @throws SQLException 
	 * 		if there was an error connecting to the database or querying it (e.g. table does not exist)
	 * @throws NotEnoughKeyLeftException
	 * 		if there is not enough key material left after the 
	 * 		current index to return n bytes of key material
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static byte[] getNextKeyBytes(String keyID, int nbytes) throws SQLException, NotEnoughKeyLeftException, NoKeyWithThatIDException {
		int index = getIndex(keyID);
		return getKeyBytesAtIndexN(keyID, nbytes, index);
	}
	
	/**
	 * Gets the next n bytes of key material for the specified key ID,
	 * starting at a given index. No guarantee that these bytes have not
	 * been used yet, so for encryption it is encouraged to use {@link #getNextKeyBytes(String, int)}.
	 * @param keyID
	 * 		ID of the key to retrieve the bytes from
	 * @param nbytes
	 * 		amount of bytes to retrieve <br>
	 * 		must be greater than 0
	 * @param startingIndex
	 * 		index at which to start retrieving bytes <br>
	 * 		must be 0 or greater
	 * @return
	 * 		the next n bytes of the key with the specified ID, 
	 * 		starting at the specified index in the key
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 * @throws NotEnoughKeyLeftException
	 * 		if there is not enough key material left after the 
	 * 		given index to return n bytes of key material
	 */
	public static byte[] getKeyBytesAtIndexN(String keyID, int nbytes, int startingIndex) throws SQLException, NoKeyWithThatIDException, NotEnoughKeyLeftException {
		if (nbytes <= 0) throw new IllegalArgumentException("Requested byte amount must be at least 1.");
		try (Connection connection = connect()) {
			// get the specified key
			String queryString = "SELECT Key FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement queryStatement = connection.prepareStatement(queryString);
			queryStatement.setString(1, keyID);
			ResultSet rs = queryStatement.executeQuery();	
			if (!rs.isBeforeFirst()) { // if result set is empty
				throw new NoKeyWithThatIDException("No key with ID " + keyID + " in the database.");
			} else {
				byte[]	key	= rs.getBytes(1);
				// check that there are enough bytes left
				if (key.length - startingIndex < nbytes) {
					throw new NotEnoughKeyLeftException(keyID, nbytes, key.length - startingIndex);
				} else {
					// isolate the first n bytes as the return array
					byte[] outBytes = Arrays.copyOfRange(key, startingIndex, startingIndex + nbytes);
					// return the requested bytes
					return outBytes;
				}
			}	
		}
	}
	
	/**
	 * Increments the key index, marking more of the key bytes as used.
	 * @param keyID
	 * 		ID of the key whose index is to be incremented
	 * @param bytes
	 * 		how much to increment the index / how many bytes are to be marked as used <br>
	 * 		if this exceeds the amount of remaining usable key bytes, all key bytes are marked as used
	 * @throws SQLException 
	 * 		if an error occurred connecting to the database, or while querying or updating it
	 * @throws NoKeyWithThatIDException 
	 * 		if the key specified by {@code keyID} has no entry in the database
	 */
	public static void incrementIndex(String keyID, int bytes) throws SQLException, NoKeyWithThatIDException {
		try (Connection connection = connect()) {
			// Get current Index for the specified key
			String queryStmtStr = "SELECT KeyIndex, KeyLength FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement queryStmt = connection.prepareStatement(queryStmtStr);
			queryStmt.setString(1, keyID);
			ResultSet rs = queryStmt.executeQuery();
			
			if (!rs.isBeforeFirst()) { // rs empty ==> no such key in DB
				throw new NoKeyWithThatIDException("No key with ID " + keyID + " in the database. Could not increment index.");
			} else {
				// Increment index to at most keyLength 
				int index = rs.getInt(1);
				int keyLengthMax = rs.getInt(2);
				int newIndex = Math.min(keyLengthMax, index + bytes);
				// Update
				String updateStmtStr = "UPDATE " + tableName + " SET KeyIndex = ? WHERE KeyID = ? ";
				PreparedStatement updateStmt = connection.prepareStatement(updateStmtStr);
				updateStmt.setInt(1, newIndex);
				updateStmt.setString(2, keyID);
				updateStmt.executeUpdate();
			}
		}
	}
	
	/**
	 * Gets how many bytes of key are left.
	 * @param keyID
	 * 		the key to check
	 * @return
	 * 		how many bytes of unused key are left for this key <br>
	 * 		0 if no key with that ID in the DB
	 * @throws SQLException 
	 * 		if an error occurred trying to connect to the DB or while querying it
	 */
	public static int getRemainingBytes(String keyID) throws SQLException {
		try (Connection connection = connect()) {
			String queryStmtStr = "SELECT KeyIndex, KeyLength FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement queryStmt = connection.prepareStatement(queryStmtStr);
			queryStmt.setString(1, keyID);
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
	 * Deletes the entry for the specified ID if it exists.
	 * @param keyID
	 * 		ID of the key to be deleted
	 * @throws SQLException 
	 * 		if there was an error connecting to the database, or an error while deleting the entry
	 */
	public static void deleteEntryIfExists(String keyID) throws SQLException {
		try (Connection connection = connect()) {
			String deleteString = "DELETE FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement deleteStatement = connection.prepareStatement(deleteString);
			deleteStatement.setString(1, keyID);
			deleteStatement.executeUpdate();
		}
	}

	/**
	 * Current index of a key.
	 * @param keyID
	 * 		the ID of the key
	 * @return
	 * 		the current index of the specified key
	 * @throws NoKeyWithThatIDException 
	 * 		if there is no key saved with that ID
	 * @throws SQLException 
	 * 		if there was an error connecting to the database, or querying for the key index
	 */
	public static int getIndex(String keyID) throws NoKeyWithThatIDException, SQLException {
		try (Connection connection = connect()) {
			String queryString = "SELECT KeyIndex FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement queryStatement = connection.prepareStatement(queryString);
			queryStatement.setString(1, keyID);
			ResultSet rs = queryStatement.executeQuery();
			if (!rs.isBeforeFirst()) { //if rs is empty
			 	throw new NoKeyWithThatIDException("No key with ID " + keyID + " in the database.");
			} else { // if entry for contact is found
				return rs.getInt(1);
			}
		}
	}
	
	/**
	 * Sets the index associated with a key to a specific value. <br>
	 * @param keyID
	 * 		the ID of the key
	 * @param index
	 * 		the new Index <br>
	 * 		if this is greater than the total length of the key, 
	 * 		the new index will instead be set to the maximum acceptable value (==keylength)
	 * @throws SQLException 
	 * 		if there was an error connecting to the Database or querying it
	 * @throws NoKeyWithThatIDException 
	 * 		if no key with the given ID exists in the database
	 */
	public static void setIndex(String keyID, int index) throws SQLException, NoKeyWithThatIDException {
		try (Connection connection = connect()) {
			// Get current Index for the specified key
			String queryStmtStr = "SELECT KeyLength FROM " + tableName + " WHERE KeyID = ?";
			PreparedStatement queryStmt = connection.prepareStatement(queryStmtStr);
			queryStmt.setString(1, keyID);
			ResultSet rs = queryStmt.executeQuery();
			
			if (!rs.isBeforeFirst()) { // rs empty ==> no such key in DB
				throw new NoKeyWithThatIDException("No key with ID " + keyID + " in the database. Could not increment index.");
			} else {
				int keyLengthMax = rs.getInt(1);
				// Update
				String updateStmtStr = "UPDATE " + tableName + " SET KeyIndex = ? WHERE KeyID = ? ";
				PreparedStatement updateStmt = connection.prepareStatement(updateStmtStr);
				updateStmt.setInt(1, Math.min(keyLengthMax, index));
				updateStmt.setString(2, keyID);
				updateStmt.executeUpdate();
			}
		}
	}
	
	/*
	 * Might be useful to have a method for changing the ID of a key?
	 */
}
