package keyStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import exceptions.NoKeyWithThatIDException;
import exceptions.NotEnoughKeyLeftException;
import frame.Configuration;

/**
 * This class supplies methods for creating, editing, getting and deleting
 * entries from the KeyStore.db which holds all the keys
 * 
 * @author Aron Hernandez, Sasha Petri
 */
public class KeyStoreDbManager {
	private static final String dataBaseName = "KeyStore.db";
	private static final String tableName = "KeyStorage";

	/**
	 * Connects to the Database.
	 * @return a new Connection to KeyStore.db
	 */
	private static Connection connect() {
		Connection con = null;

		try {
			Class.forName("org.sqlite.JDBC");

			// get base directory from the configuration to store new Database correctly
			String currentPath = Configuration.getBaseDirPath();

			con = DriverManager.getConnection("jdbc:sqlite:" + currentPath + dataBaseName); // connect to our db

		} catch (ClassNotFoundException | SQLException e) {

			System.err.println("Connection to database failed!" + "\n");
			System.err.println(e.toString());

		}

		return con;
	}

	/**
	 * Creates a new Database and table in the current project directory folder,
	 * if they do not already exist.
	 * @return True if Database and table were created successfully, False
	 *          otherwise
	 * @throws SQLException if there was an error connecting to the database or creating the table
	 */
	public static void createNewKeyStoreAndTable() throws SQLException {
		try (Connection conn = KeyStoreDbManager.connect()) {
			Statement stmnt = conn.createStatement();

			System.out.println("Database was created successfully!");

			// create Table
			String keyInformationSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (KeyStreamId CHAR(128) UNIQUE ,"
					+ " KeyBuffer INTEGER , " + " Index_ INTEGER NOT NULL , " + " Source_ TEXT NOT NULL, "
					+ " Destination TEXT NOT NULL, " + " Used BOOLEAN NOT NULL, " + " Initiative BOOLEAN NOT NULL, "
					+ "PRIMARY KEY (KeyStreamId))";

			stmnt.executeUpdate(keyInformationSQL);
			System.out.println("Table creation successful!");
		}
	}

	/**
	 *
	 * ---> Method inserts a new Entry in the keystore
	 *
	 * @param keyStreamID reference ID to locate a key
	 * @param keyBuffer   byte[] containing the key
	 * @param source      identifier for the source application
	 * @param destination identifier for the destination application
	 * @param used        boolean parameter signaling whether a key has been used already
	 * @param initiative  boolean parameter signaling whether this Entry/Person has the initiative <br>
	 * 					  an entity has the initiative if it started the key generation
	 * @throws SQLException 
	 * 		if there was an error with the database
	 */
	public static void insertToKeyStore(String keyStreamID, byte[] keyBuffer, String source, String destination,
			boolean used, boolean initiative) throws SQLException {
		// if there is no database yet, create one
		createNewKeyStoreAndTable();

		// set index to 0 because no part of the key has been used
		int index = 0;
		try (Connection conn = connect()) {
			String sql = "INSERT INTO " + tableName
					+ "(KeyStreamID, KeyBuffer, Index_, Source_, Destination, Used, Initiative)  VALUES(?,?,?,?,?,?,?)";

			PreparedStatement prepStmnt = conn.prepareStatement(sql);

			prepStmnt.setString(1, keyStreamID);
			prepStmnt.setBytes(2, keyBuffer);
			prepStmnt.setInt(3, index);
			prepStmnt.setString(4, source);
			prepStmnt.setString(5, destination);
			prepStmnt.setBoolean(6, used);
			prepStmnt.setBoolean(7, initiative);

			prepStmnt.executeUpdate();
			System.out.println("Insertion to KeyInformation table was successful.");
		}

	}

	/**
	 * Changes the key saved under a certain ID.
	 * @param keyStreamID 
	 * 		reference ID to locate a Key.
	 * @param key         
	 * 		the new Key as a byte[]
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static void changeKeyBuffer(String keyStreamID, byte[] key)
			throws NoKeyWithThatIDException, SQLException {

		try (Connection conn = connect()) {
			String sql = "UPDATE " + tableName + " SET KeyBuffer = ? WHERE KeyStreamID = ?";
			PreparedStatement pstmnt = conn.prepareStatement(sql);
			pstmnt.setBytes(1, key);
			pstmnt.setString(2, keyStreamID);
			pstmnt.executeUpdate();

		}

	}

	/**
	 * Checks if there is a specified amount of key material left for the key with the given ID.
	 * @param keyStreamID 
	 * 		reference ID to locate a key
	 * @param keyLength
	 * 		how much key material to check for
	 * @return true if the key has the specified amount of key material left, false otherwise
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static boolean enoughKeyMaterialLeft(String keyStreamID, int keyLength)
			throws NoKeyWithThatIDException, SQLException {

		KeyStoreObject currentObject = getEntryFromKeyStore(keyStreamID);
		int index = currentObject.getIndex();
		if (index == 0) {
			index = keyLength;
		}

		int totalBits = currentObject.getCompleteKeyBuffer().length;

		int bitsLeft = totalBits - index;
	
		if (bitsLeft >= keyLength) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Updates the index parameter of a key stored in the DB.
	 * @param keyStreamID 
	 * 		reference ID to locate a key
	 * @param newIndex   
	 * 		the new integer value of the index <br>
	 * 		may not be negative, and may not be greater than the total key length of the specified key
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 * @throws NotEnoughKeyLeftException 
	 * 		if the index is greater than the total key length, 
	 * 		i.e. setting the index to be the specified value
	 * 		would result in an invalid key store entry
	 */
	public static boolean changeIndex(String keyStreamID, int newIndex) throws NoKeyWithThatIDException, SQLException, NotEnoughKeyLeftException {
		KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
		if (newIndex > obj.getCompleteKeyBuffer().length) {
			throw new NotEnoughKeyLeftException("Can not set Index to " + newIndex + " for key with ID " + keyStreamID + ". "
					+ "Index can at most be " + obj.getCompleteKeyBuffer().length);
		} else {
			try (Connection conn = connect()) {
				

				String sql = "UPDATE " + tableName + " SET Index_ = ? WHERE KeyStreamID = ?";
				PreparedStatement pstmnt = conn.prepareStatement(sql);

				pstmnt.setInt(1, newIndex);
				pstmnt.setString(2, keyStreamID);
				pstmnt.executeUpdate();

				pstmnt.close();
				conn.close();

				int index = KeyStoreDbManager.getEntryFromKeyStore(keyStreamID).getIndex();
				System.out.println("Successfully updated the Index to " + index);

				return true;

			} catch (SQLException e) {
				System.err.println("Changing the Index of Entry failed! " + "\n");
				System.err.println(e.toString());
				return false;
			}
		}
		
	}
	
	/**
	 * Gets the current index of a key. <br>
	 * Currently, keys are byte indexed, i.e. index 3 means that the first 3 bytes
	 * of the key byte array have been used.
	 * 
	 * @param keyStreamID ID of the key to get the index for
	 * @return current index for the key associated with the given ID
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static int getIndex(String keyStreamID) throws NoKeyWithThatIDException, SQLException {
		KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
		return obj.getIndex();
	}

	/**
	 * Increments the index of the specified key by the given amount. <br>
	 * If this would exceed the total length of the key, the index is instead set to
	 * the max key length.
	 * 
	 * @param keyStreamID ID of the key whose index should be incremented
	 * @param increment   how much to increment the index, must be > 0
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static void incrementIndex(String keyStreamID, int increment) throws NoKeyWithThatIDException, SQLException {
		if (increment <= 0)
			throw new IllegalArgumentException("Specified increment must be at least 1, but was " + increment);
		KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
		int newIndex = Math.min(obj.getIndex() + increment, obj.getCompleteKeyBuffer().length); // new index is at most == key length
		try {
			changeIndex(keyStreamID, newIndex);
		} catch (NotEnoughKeyLeftException e) {
			// never thrown
		}
	}

	/**
	 * Displays all entries on the console
	 *
	 * @return True if output was displayed correctly, False otherwise
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static void selectAll() throws SQLException {
		
		try (Connection conn = connect();) {
			String sql = "SELECT * FROM " + tableName;
			Statement stmnt = conn.createStatement();
			ResultSet result = stmnt.executeQuery(sql);

			while (result.next()) {
				System.out.println(result.getString("KeyStreamID") + "\t" + result.getInt("KeyBuffer") + "\t"
						+ result.getInt("Index_") + "\t" + result.getString("Source_") + "\t"
						+ result.getString("Destination"));
			}
		}
			
	}

	/**
	 * Deletes an entry in the key store if it exists.
	 * @param keyStreamID 
	 * 		the ID of the key that should be deleted from the DB
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static void deleteEntryIfExists(String keyStreamID) throws SQLException {

		try (Connection conn = connect()) {
			String sql = "DELETE FROM " + tableName + " WHERE KeyStreamId= ?";
			PreparedStatement pstmnt = conn.prepareStatement(sql);

			pstmnt.setString(1, keyStreamID);
			pstmnt.executeUpdate();
			System.out.println("Entry from KeyInformation table was deleted successfully!");
		}

	}

	/**
	 * Deletes all entries in the keystore which are marked as "used".
	 * @return 	True if all the used entries are deleted, 
	 * 			False if there are none to delete
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static boolean deleteUsedKeys() throws SQLException {

		List<String> keyIdList = KeyStoreDbManager.getKeyStoreAsList().stream().filter(obj -> obj.isUsed() == true)
				.map(obj -> new String(obj.getID())).collect(Collectors.toList());

		if (keyIdList.size() == 0) {
			System.err.println("There are no used keys that could be deleted");
			return false;
		}

		for (String str : keyIdList) {
			deleteEntryIfExists(str);
		}
		
		return true;

	}

	/**
	 * Change the "Used" parameter of a KeyStore Entry from unused(=0) to used(=1)
	 *
	 * @param keyStreamID reference ID for a Key
	 * @return true if operation succeeded, false otherwise
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database
	 */
	public static boolean changeKeyToUsed(String keyStreamID) throws SQLException, NoKeyWithThatIDException {
		
		if (!doesKeyStreamIdExist(keyStreamID)) 
			throw new NoKeyWithThatIDException("No key with ID " + keyStreamID + " exists in the keystore.");

		try (Connection conn = connect()) {

			String sql = "UPDATE " + tableName + " SET Used = 1 WHERE KeyStreamID = ?";
			PreparedStatement pstmnt = conn.prepareStatement(sql);

			pstmnt.setString(1, keyStreamID);
			pstmnt.executeUpdate();
			System.out.println("Changed key from Unused to Used");

			return true;

		} 
	}

	/**
	 * Gets a {@linkplain KeyStoreObject} corresponding to the specified key.
	 * Please note that changes to the key store object are not reflected on the database,
	 * and changes in the database are not reflected in the object, until a new object
	 * for the key is acquired through re-running this method.
	 * @param keyStreamID 
	 * 		identifier of the key that needs to be wrapped in a KeyStoreObject
	 * @return 
	 * 		a new KeyInformationObject containing all the KeyInformation from the Entry with corresponding KeyStreamId
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database 
	 */
	public static KeyStoreObject getEntryFromKeyStore(String keyStreamID) // TODO? Could potentially return null instead of NoKeyException, would need minor adjustments elsewhere
			throws NoKeyWithThatIDException, SQLException {
		if (!doesKeyStreamIdExist(keyStreamID)) {
			throw new NoKeyWithThatIDException("There is no key in the keystore with ID " + keyStreamID);
		}

		try (Connection conn = connect();) {

			String sql = "SELECT * FROM " + tableName + " WHERE KeyStreamId = " + "\"" + keyStreamID + "\"";

			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);

			KeyStoreObject object = new KeyStoreObject(rs.getString("KeyStreamId"), rs.getBytes("KeyBuffer"),
					rs.getInt("Index_"), rs.getString("Source_"), rs.getString("Destination"), rs.getBoolean("Used"),
					rs.getBoolean("Initiative"));


			return object;

		}
	}


	/**
	 * Returns an ArrayList of {@linkplain KeyStoreObject}s representing all entries in the keystore.
	 *
	 * @return an ArrayList of KeyInformationObject which contain information about
	 *         the keys currently in storage, may be empty
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static ArrayList<KeyStoreObject> getKeyStoreAsList() throws SQLException {
		try (Connection conn = connect()) {
			
			String sql = "SELECT * FROM " + tableName;
			PreparedStatement stmnt = conn.prepareStatement(sql);
			ResultSet rs = stmnt.executeQuery();
			ArrayList<KeyStoreObject> result = new ArrayList<>();
			while (rs.next()) {
				KeyStoreObject res = new KeyStoreObject(rs.getString("KeyStreamID"), rs.getBytes("KeyBuffer"),
						rs.getInt("Index_"), rs.getString("Source_"), rs.getString("Destination"),
						rs.getBoolean("Used"), rs.getBoolean("Initiative"));
				result.add(res);
			}


			return result;
		} 
	}

	/**
	 * Check whether an entry for this keyStreamId exists in the KeyStore or not.
	 * @param keyStreamID 
	 * 		reference ID to locate a key
	 * @return true 
	 * 		if an entry with the given keyStreamID exists, false otherwise
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static boolean doesKeyStreamIdExist(String keyStreamID) throws SQLException {
		List<String> keyIdList = KeyStoreDbManager.getKeyStoreAsList().stream().map(obj -> new String(obj.getID()))
				.collect(Collectors.toList());

		return keyIdList.contains(keyStreamID);
	}



	/**
	 * Gets the next n bytes of key material of the specified key. May increment it.
	 * 
	 * @param keyStreamID the key to retrieve key material from
	 * @param nbytes      how many bytes of key material to retrieve, must be >= 0
	 * @param increment   true if the key should be incremented (increases index by n)
	 * @return
	 * 		the next n bytes of the key with the specified ID, 
	 * 		starting at the current index of the key
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database 
	 * @throws NotEnoughKeyLeftException
	 * 		if there is not enough key material left after the 
	 * 		current index to return n bytes of key material
	 */
	public static byte[] getNextNBytes(String keyStreamID, int nbytes, boolean increment)
			throws NotEnoughKeyLeftException, NoKeyWithThatIDException, SQLException {
		byte[] out = getKeyBytesAtIndexN(keyStreamID, nbytes, getIndex(keyStreamID));
		if (increment)
			incrementIndex(keyStreamID, nbytes);
		return out;
	}

	/**
	 * Gets n bytes of key material starting at a specified index in the key
	 * with the given key stream ID. Use of this to retrieve material for
	 * encryption is discouraged, instead use {@link #getNextNBytes(String, int, boolean)}.
	 * This method is intended to be mainly used for getting keys for decryption.
	 * @param keyStreamID
	 * 		ID used to identify the key to retrieve bytes from
	 * @param nbytes
	 * 		how many bytes to retrieve, must be > 0
	 * @param index
	 * 		the index to start at, must be >= 0
	 * @return
	 * 		keyBuffer[index] to keyBuffer[index + nbytes] of the specified key
	 * @throws NotEnoughKeyLeftException
	 * 		if there is not enough key material left after the 
	 * 		specified index to return n bytes of key material		
	 * @throws NoKeyWithThatIDException
	 * 		if no key with the specified ID could be found in the database 
	 * @throws SQLException
	 * 		if an error occurred with the SQL database this key manager is based on (e.g. table doesn't exist)
	 */
	public static byte[] getKeyBytesAtIndexN(String keyStreamID, int nbytes, int index)
			throws NotEnoughKeyLeftException, NoKeyWithThatIDException, SQLException {
		if (index < 0) throw new IndexOutOfBoundsException("Index may not be less than 0, but was " + index);
		if (nbytes <= 0) throw new IllegalArgumentException("Must specify an amount of bytes to get greater than 0, but specified " + nbytes);
		KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);

		if (obj.getCompleteKeyBuffer().length < index + nbytes)
			throw new NotEnoughKeyLeftException("Can't return " + nbytes + " bytes of key material for key <"
					+ keyStreamID + "> " + " starting at index <" + index + ">. Only "
					+ (obj.getCompleteKeyBuffer().length - index) + " bytes are left.");

		return Arrays.copyOfRange(obj.getCompleteKeyBuffer(), index, index + nbytes);
	}

	/*
	 * ------ API Methods ------
	 */

	/**
	 * from etsi paper: Reserve an association (Key_stream_ID) for a set of future
	 * keys at both ends of the QKD link through this distributed QKD key management
	 * layer and establish a set of parameters that define the expected levels of
	 * key service. This function shall block until the peers are connected or until
	 * the Timeout value in the QoS parameter is exceeded.
	 *
	 * @param source        identifier for the source application
	 * @param destination   identifier for the destination application
	 * @param qosParameters multiple parameters that are delivered as a QoS Object
	 *                      //Weiß selber ned wie der Parameter aussehen soll
	 * @param keyStreamID   reference ID to locate a key
	 * @return number of status
	 * @throws SQLException 
	 */
	public static int open_Connect(String source, String destination, QoS qosParameters, String keyStreamID,
			boolean peerConnected, boolean initiative) throws SQLException {

		// return 5 -----> if KeyStramID already exists
		if (!doesKeyStreamIdExist(keyStreamID)) {
			return 5;
		}

		insertToKeyStore(keyStreamID, null, source, destination, false, initiative);
		boolean insertbool = doesKeyStreamIdExist(keyStreamID);

		if (insertbool) {
			// return 0 -----> if everything was succesfull
			if (peerConnected)
				return 0;

			// return 1 -----> if connection was established but peer is not connected
			return 1;

		} else {
			return -1; // indicating that something went wrong
		}

		// TODO

		// return 6 -----> if the timeout parameter is exceeded -----> when is it
		// exceeded tho?

		// return 7 -----> if OPEN failed because requested QoS settings could not be
		// met, counter proposal included in return has occurred

	}

	/**
	 * from etsi: Terminate the association established for this Key_stream_ID. No
	 * further keys shall be allocated for this Key_stream_ID after the association
	 * has been closed. Due to timing differences at the other end of the link this
	 * peer operation will happen at some other time and any unused keys shall be
	 * held until that occurs and then discarded or the TTL (Time To Live QoS
	 * parameter) has expired.
	 *
	 * @param keyStreamID reference ID to locate a key
	 * @return number of status
	 * @throws SQLException
	 */
	static int close(String keyStreamID, boolean peerConnected) throws SQLException {
		// delete keyInformation for this ID

		// return 0 -----> if everything was succesfull
		if (doesKeyStreamIdExist(keyStreamID)) {
			deleteEntryIfExists(keyStreamID);
			if (peerConnected)
				return 0;
			return 1;
		}
		System.err.println("There is no Entry with this KeyStreamID" + "\n");
		return -1; // -1 = new Status parameter indicating that the operation failed

	}

	/**
	 * Etsi paper page 9 latest version for more information
	 *
	 * @param keyStreamID reference ID to locate a key
	 * @return number of status + the key
	 * @throws SQLException
	 * @throws NoKeyWithThatIDException
	 */
	public static Map.Entry<byte[], Integer> get_Key(String keyStreamID) throws NoKeyWithThatIDException, SQLException {
		AbstractMap.SimpleEntry<byte[], Integer> pair;

		if (doesKeyStreamIdExist(keyStreamID)) {
			KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
			int index = obj.getIndex();
			byte[] key = obj.getCompleteKeyBuffer();

			return new AbstractMap.SimpleEntry<byte[], Integer>(key, index);
		}

		System.err.println("There is no Entry with this keyStreamID!");
		return null;

	}

}
