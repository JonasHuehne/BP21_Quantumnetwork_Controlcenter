package keyStore;

import frame.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class supplies methods for creating, editing, getting and deleting entries from the KeyStore.db which holds all the keys
 * @author Aron Hernandez
 */

public class KeyStoreDbManager {
    private static final String dataBaseName = "KeyStore.db";
    private static final String tableName = "KeyStorage";
    
    /**
     *
     * @return a new Connection to KeyStore.db
     */
    private static Connection connect() {
        Connection con = null;

        try {
            Class.forName("org.sqlite.JDBC");

            // get base directory from the configuration to store new Database correctly
            String currentPath = Configuration.getBaseDirPath();

            con = DriverManager.getConnection("jdbc:sqlite:" + currentPath + dataBaseName); // connect to our db
            //System.out.println("Connection to database was succesfull!");

        } catch (ClassNotFoundException | SQLException e) {

            System.err.println("Connection to database failed!" + "\n");
            System.err.println(e.toString());

        }

        return con;
    }

    /**
     * Creates a new Database in the current project directory folder
     *
     * @returns True if Database and table were created successfully, False otherwise
     */
    public static boolean createNewKeyStoreAndTable() {

        try {
            Connection conn = KeyStoreDbManager.connect();
            Statement stmnt = conn.createStatement();

            System.out.println("Database was created successfully!");

            // create Table
            String keyInformationSQL = "CREATE TABLE IF NOT EXISTS " + tableName +
                    " (KeyStreamId CHAR(128) UNIQUE ," +
                    " KeyBuffer INTEGER UNIQUE , " +
                    " Index_ INTEGER NOT NULL , " +
                    " Source_ TEXT NOT NULL, " +
                    " Destination TEXT NOT NULL, " +
                    " Used BOOLEAN NOT NULL, " +
                    " Initiative BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (KeyStreamId))";

            stmnt.executeUpdate(keyInformationSQL);
            System.out.println("Table creation successful!");

            stmnt.close();
            conn.close();

            return true;

        } catch (SQLException e) {
            System.err.println("Database creation failed!" + "\n");
            System.err.println(e.toString());

            return false;
        }
    }

    /**
     *
     * ---> Method inserts a new Entry in the keystore
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyBuffer byte[] containing the key
     * @param source identifier for the source application
     * @param destination identifier for the destination application
     * @param used boolean parameter signaling whether a key has been used already
     * @param initiative boolean parameter signaling whether this Entry/Person has the initiative
     */

    public static boolean insertToKeyStore(String keyStreamID, byte[] keyBuffer, String source, String destination, boolean used, boolean initiative){

        // set index to 0 because no part of the key has been used
        int index = 0;
        try{
            Connection conn = connect();

            String sql = "INSERT INTO " + tableName + "(KeyStreamID, KeyBuffer, Index_, Source_, Destination, Used, Initiative)  VALUES(?,?,?,?,?,?,?)";

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

            prepStmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e ){
            System.err.println("Insertion to KeyInformation table failed!" + "\n");
            System.err.println(e.toString());

        }
        return false;
    }

    /**
     *
     * @param keyStreamID reference ID to locate a Key.
     * @param key the new Key as a byte[]
     * @return True if operation was successful, false otherwise.
     */

    public static boolean changeKeyBuffer(String keyStreamID, byte[] key){

        if (!doesKeyStreamIdExist(keyStreamID)){
            System.err.println("Unable to add key as there is no Entry with this KeyStreamID!" + "\n");
            return false;
        }
        KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);

            try {
                Connection conn = connect();
                String sql = "UPDATE " + tableName + " SET KeyBuffer = ? WHERE KeyStreamID = ?";
                PreparedStatement pstmnt = conn.prepareStatement(sql);
                pstmnt.setBytes(1, key);
                pstmnt.setString(2, keyStreamID);
                pstmnt.executeUpdate();

                pstmnt.close();
                conn.close();
                System.out.println("Renaming entry in KeyInformation table succeeded.\" + \"\\n\"");
                return true;

            } catch (SQLException e) {
                System.err.println("Renaming entry in KeyInformation table failed!" + "\n");
                System.err.println(e.toString());

                return false;
            }

    }

    /** checks if there is enough key material left, so it can be used (at least 256 Bits or 32 Byte).
     * If there is not enough key material left, the entry shall be marked as used:
     *
     *
     * @param keyStreamID reference ID to locate a key
     * @return True if there still is key material that can be used, False otherwise
     */
    public static boolean enoughKeyMaterialLeft(String keyStreamID, int keyLength){
        if (!doesKeyStreamIdExist(keyStreamID)){
            System.err.println("Unable to change the Index as there is no Entry with this KeyStreamID!" + "\n");
            return false;
        }

        KeyStoreObject currentObject = getEntryFromKeyStore(keyStreamID);
        int index = currentObject.getIndex();
        if(index == 0){
            index = keyLength;
        }

        int totalBits = currentObject.getCompleteKeyBuffer().length;
        //System.out.println("total bytes = " + totalBits );

        int bitsLeft = totalBits - index  ;
        //System.out.println("bitsLeft = " + bitsLeft);

        if (bitsLeft >= keyLength){
            return true;
        }
        else{

            return false;
        }
    }

    /** method for updating/changing the Index parameter of a Keystore entry.
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyLength integer value indicating length of key in bits
     * @return True if index was changed successfully, False otherwise
     */
    public static boolean changeIndex(String keyStreamID, int keyLength){
        if (!doesKeyStreamIdExist(keyStreamID)){
            System.err.println("Unable to change the Index as there is no Entry with this KeyStreamID!" + "\n");
            return false;
        }
        KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
        int currentIndex = obj.getIndex();

        int newIndex = currentIndex + keyLength;

        if(!enoughKeyMaterialLeft(keyStreamID, keyLength)){

            // key will be set to used
            System.err.println("key will be set to used as there is not enough key material left!" + "\n");
            changeKeyToUsed(keyStreamID);
            return false;
        }

        try {
            Connection conn = connect();

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



    /** Displays all entries on the console
     *
     * @return True if output was displayed correctly, False otherwise
     */
    public static boolean selectAll(){

        try {
            String sql = "SELECT * FROM " + tableName;
            Connection conn = connect();
            Statement stmnt = conn.createStatement();
            ResultSet result = stmnt.executeQuery(sql);

            while(result.next()){
                System.out.println(result.getString("KeyStreamID") +  "\t" +
                        result.getInt("KeyBuffer") +  "\t" +
                        result.getInt("Index_") +  "\t" +
                        result.getString("Source_")+  "\t" +
                        result.getString("Destination"));
            }

            stmnt.close();
            conn.close();
            return true;
        } catch (SQLException e) {
            System.err.println( "Selecting every entry from KeyStore.db failed!" );
            System.err.println(e.toString());
            return false;
        }
    }

    /**
     *
     * @param keyStreamID the ID of the key that needs to be deleted from the DB
     * @return True if operation was successful, False otherwise
     */
    public static boolean deleteKeyInformationByID(String keyStreamID){
        if (!KeyStoreDbManager.doesKeyStreamIdExist(keyStreamID)){
            System.err.println("Deleting Key entry from DB failed because the given keyStreamID does not exist!"  + "\n");
            return false;
        }

        try {
            String sql = "DELETE FROM " + tableName + " WHERE KeyStreamId= ?";

            Connection conn = connect();
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            pstmnt.executeUpdate();
            System.out.println("Entry from KeyInformation table was deleted successfully!");
            pstmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e) {
            System.err.println("Deleting entry from KeyInformation table failed!" + "\n");
            System.err.println(e.toString());
            return false;
        }

    }

    /** Method for deleting all used keys in one go
     *
     * @return True if all the used entries are deleted, False if there are non to be deleted
     */
    public static boolean deleteUsedKeys(){

        List<String> keyIdList = KeyStoreDbManager.getKeyStoreAsList().stream().filter(obj -> obj.getUsed() == true).map(obj -> new String(obj.getID())).collect(Collectors.toList());

        if(keyIdList.size() == 0 ){
            System.err.println("There are no used keys that could be deleted");
            return  false;
        }

        for(String str: keyIdList){
            // try/catch block hier überhaupt nötig?
            try {
                deleteKeyInformationByID(str);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
        return true;

    }

    /** Change the "Used" parameter of a KeyStore Entry from unused(=0) to used(=1)
     *
     * @param keyStreamID reference ID for a Key
     * @return true if operation succeeded, false otherwise
     */
    public static boolean changeKeyToUsed(String keyStreamID){

        if (!doesKeyStreamIdExist(keyStreamID)){
            System.err.println("Unable to update used parameter as there is no Entry with this KeyStreamID" + "\n");
            return false;
        }

        try {
            Connection conn = connect();

            String sql = "UPDATE " + tableName + " SET Used = 1 WHERE KeyStreamID = ?";
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            pstmnt.executeUpdate();
            System.out.println("Changed key from Unused to Used");
            pstmnt.close();
            conn.close();

            return true;

        } catch (SQLException e) {
            System.err.println("Changing the used Parameter of Entry failed " + "\n");
            System.err.println(e.toString());
            return false;
        }
    }

    /** Get a new KeyInformationObject by the corresponding keyStreamID
     *
     * @param keyStreamID identifier of the key that needs to be wrapped in a KeyStoreObject
     * @return a new KeyInformationObject containing all the KeyInformation from the Entry with corresponding KeyStreamId
     */
     public static KeyStoreObject getEntryFromKeyStore(String keyStreamID) {
         if (!doesKeyStreamIdExist(keyStreamID)){
             System.err.println("There is no Entry with this KeyStreamID!"  + "\n");
             return null;
         }

        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + tableName + " WHERE KeyStreamId = " + "\"" + keyStreamID + "\"";

            Statement stmnt = conn.createStatement();

            ResultSet rs = stmnt.executeQuery(sql);

            KeyStoreObject object = new KeyStoreObject(rs.getString("KeyStreamId"),
                    rs.getBytes("KeyBuffer"), rs.getInt("Index_"),
                    rs.getString("Source_"), rs.getString("Destination"),
                    rs.getBoolean("Used"),  rs.getBoolean("Initiative"));

            stmnt.close();
            conn.close();
            //System.out.println("Selecting entry from KeyInformation table was successful!" + "\n");

            return object;


        } catch (SQLException e) {
            System.err.println("Selecting entry from KeyInformation table failed!" + "\n");
            System.err.println(e.toString());
            return null;
        }
    }

    /** Stores all the Entries of the KeyInformation table in an ArrayList and returns the List
     *
     * @return a ArrayList of KeyInformationObject which contain information about the keys currently in storage
     */
     public static ArrayList<KeyStoreObject> getKeyStoreAsList() {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + tableName;
            PreparedStatement stmnt = conn.prepareStatement(sql);
            ResultSet rs = stmnt.executeQuery();
            ArrayList<KeyStoreObject> result = new ArrayList<>();
            while(rs.next()) {
                KeyStoreObject res = new KeyStoreObject(rs.getString("KeyStreamID"),
                        rs.getBytes("KeyBuffer"), rs.getInt("Index_"),
                        rs.getString("Source_"), rs.getString("Destination"),
                        rs.getBoolean("Used"),  rs.getBoolean("Initiative"));
                result.add(res);
            }
            stmnt.close();
            conn.close();

            //System.out.println("Generating list of KeyInformation table entries was successful" + "\n");
  
            return result;
        } catch (SQLException e) {
            System.err.println("Generating list of all entries from KeyInformation table failed!");
            System.err.println(e.toString());
            return null;
        }
    }

    /** Check whether this keyStreamId exists in the KeyStore or not.
     *
     * @param keyStreamID reference ID to locate a key
     * @return true if the keyStreamID exists, false otherwise
     */
   public static boolean doesKeyStreamIdExist(String keyStreamID){
        List<String> keyIdList = KeyStoreDbManager.getKeyStoreAsList().stream().map(obj -> new String(obj.getID())).collect(Collectors.toList());

        return keyIdList.contains(keyStreamID);
    }

    /*
     *  ------ API Methods ------
     */

    /** from etsi paper: Reserve an association (Key_stream_ID) for a set of future keys at both ends of the QKD link
     through this distributed QKD key management layer and establish a set of parameters that
     define the expected levels of key service. This function shall block until the peers are
     connected or until the Timeout value in the QoS parameter is exceeded.
     *
     * @param source identifier for the source application
     * @param destination identifier for the destination application
     * @param qosParameters multiple parameters that are delivered as a QoS Object                                       //Weiß selber ned wie der Parameter aussehen soll
     * @param keyStreamID reference ID to locate a key
     * @return number of status
     */
    public static int open_Connect(String source, String destination, QoS qosParameters, String keyStreamID, boolean peerConnected, boolean initiative){

        //return 5 -----> if KeyStramID already exists
        if(!doesKeyStreamIdExist(keyStreamID)){
            return 5;
        }

        boolean insertbool = insertToKeyStore(keyStreamID, null, source, destination, false, initiative);

        if(insertbool) {
            //return 0 -----> if everything was succesfull
            if (peerConnected) return 0;

            //return 1 -----> if connection was established but peer is not connected
            return 1;

        }
        else{
            return -1; // indicating that something went wrong
        }

        //TODO

        //return 6 -----> if the timeout parameter is exceeded -----> when is it exceeded tho?

        //return 7 -----> if OPEN failed because requested QoS settings could not be met, counter proposal included in return has occurred

    }

    /** from etsi: Terminate the association established for this Key_stream_ID. No further keys shall be
     allocated for this Key_stream_ID after the association has been closed. Due to timing
     differences at the other end of the link this peer operation will happen at some other time and
     any unused keys shall be held until that occurs and then discarded or the TTL (Time To Live
     QoS parameter) has expired.
     *
     * @param keyStreamID reference ID to locate a key
     * @return number of status
     */
    static int close(String keyStreamID, boolean peerConnected){
        //delete keyInformation for this ID

        //return 0 -----> if everything was succesfull
        if (doesKeyStreamIdExist(keyStreamID)){
           deleteKeyInformationByID(keyStreamID);
           if (peerConnected) return 0;
            return 1;
        }
        System.err.println("There is no Entry with this KeyStreamID" + "\n");
        return -1; // -1 = new Status parameter indicating that the operation failed


    }

    /** Etsi paper page 9 latest version for more information
     *
     * @param keyStreamID reference ID to locate a key
     * @return number of status + the key
     */
    public static Map.Entry<byte[], Integer> get_Key(String keyStreamID) {
        AbstractMap.SimpleEntry<byte[], Integer> pair;

        if(doesKeyStreamIdExist(keyStreamID)){
            KeyStoreObject obj = getEntryFromKeyStore(keyStreamID);
            int index = obj.getIndex();
            byte[] key = obj.getCompleteKeyBuffer();

            return new AbstractMap.SimpleEntry<byte[], Integer>(key, index);
        }

        System.err.println("There is no Entry with this keyStreamID!");
        return null;


    }






}

