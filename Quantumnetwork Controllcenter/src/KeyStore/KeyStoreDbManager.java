package KeyStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class KeyStoreDbManager {
    private static final String dataBaseName = "KeyStore.db";
    private static final String infoTableName = "KeyInformation";
    private static final String keyTableName = "KeyStore";


    /**
     *
     * @return a new Connection to KeyStore.db
     */
    private static Connection connect() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC");

            // find project directory to store new Database correctly
            String currentPath = System.getProperty("user.dir");

            con = DriverManager.getConnection("jdbc:sqlite:" + currentPath + "\\" + dataBaseName); // connect to our db
            System.out.println("Connection to database was succesfull!");

        } catch (ClassNotFoundException | SQLException e) {

            System.out.println("Connection to database failed!" + "\n");
            e.printStackTrace();
        }

        return con;
    }

    /**
     * Creates a new Database in the current project directory folder
     *
     * @returns True if Database and table were created successfully, False otherwise
     */
    static boolean createNewKeyStoreAndTable() {

        try {
            Connection conn = KeyStoreDbManager.connect();
            Statement stmnt = conn.createStatement();

            System.out.println("Database was created successfully!");

            // create Table
            String keyInformationSQL = "CREATE TABLE IF NOT EXISTS " + infoTableName +
                    " (KeyStreamId CHAR(128) ," +
                    " KeyBuffer INTEGER NOT NULL, " + // Index muss definitiv noch angepasst werden, Frage im Treffen heute nach
                    " Index_ INTEGER NOT NULL , " +
                    " Source_ TEXT NOT NULL, " +
                    " Destination TEXT NOT NULL, " +
                    "PRIMARY KEY (KeyStreamId, Index_))";

            stmnt.executeUpdate(keyInformationSQL);
            System.out.println("Table creation successful!");

            stmnt.close();
            conn.close();
            //create new table that holds the real keys
            try {
                Connection conn2 = connect();
                Statement stmnt2 = conn2.createStatement();
                String keyStoreSQL = "CREATE TABLE IF NOT EXISTS " + keyTableName +
                        " (Key CHAR(256) NOT NULL," +
                        " Used INTEGER NOT NULL, " + // 0 = False , 1 = True
                        " KeyStreamID CHAR(128) NOT NULL, " +
                        "PRIMARY KEY (Key))";

                stmnt2.executeUpdate(keyStoreSQL);
            } catch (SQLException e) {
                e.printStackTrace();
            }


            return true;

        } catch (SQLException e) {
            System.out.println("Database creation failed!" + "\n");
            e.printStackTrace();

            return false;
        }
    }

    /**
     *  --------- Methods for the KeyInformation table ----------
     */


    /**Following the Etsi paper there should be 2 Tables. #1 which holds all the information about a key
     * and #2 which stores the real keys.
     *
     * ---> Method inserts a new Entry in table #1 (KeyInformation)
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyBuffer array of char bytes ()
     * @param index index for the new Key
     * @param source identifier for the source application
     * @param destination identifier for the destination application
     */
    public static boolean insertKeyInformation(String keyStreamID, int keyBuffer, int index, String source, String destination ){

        try{
            Connection conn = connect();

            String sql = "INSERT INTO KeyInformations(KeyStreamID, KeyBuffer, Index_, Source_, Destination)  VALUES(?,?,?,?,?)";

            PreparedStatement prepStmnt = conn.prepareStatement(sql);

            prepStmnt.setString(1, keyStreamID);
            prepStmnt.setInt(2, keyBuffer);
            prepStmnt.setInt(3, index);
            prepStmnt.setString(4, source);
            prepStmnt.setString(5, destination);

            prepStmnt.executeUpdate();
            System.out.println("Insertion to KeyInformation table was successful");

            prepStmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e ){
            System.out.println("Insertion to KeyInformation table failed!" + "\n" );
            e.printStackTrace();
        }
        return false;
    }

    /** (Api needs a function which reserves an empty KeyStreamID, therefore we should be able to rename one by the index (i guess))
     *
     * @param index the index of the keyStreamID to be renamed
     * @param newID new KeyStreamID
     * @return True if operation was successful, False otherwise
     */
    public static boolean updateKeyStreamID (int index, String newID) {
        try {
            Connection conn = connect();
            String sql = "UPDATE " + infoTableName + " SET KeyStreamId = ? WHERE Index_ = ?";
            PreparedStatement pstmnt = conn.prepareStatement(sql);
            pstmnt.setString(1, newID);
            pstmnt.setInt(2, index);
            pstmnt.executeUpdate();

            pstmnt.close();
            conn.close();
            System.out.println("Renaming entry in KeyInformation table succeeded!\" + \"\\n\"");
            return true;

        } catch (Exception e) {
            System.err.println("Renaming entry in KeyInformation table failed!" + "\n");
            e.printStackTrace();
            return false;
        }
    }


    /** Displays all entries on the console
     *
     * @return True if output was displayed correctly, False otherwise
     */
    static boolean selectAll(){

        try {
            String sql = "SELECT * FROM " + infoTableName;
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
            System.out.println( "Selecting every entry from KeyStore.db failed!" );
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param keyStreamID the ID of the key that needs to be deleted from the DB
     * @return True if operation was successful, False otherwise
     */
    static boolean deleteKeyInformationByID(String keyStreamID){

        try {
            String sql = "DELETE FROM " + infoTableName + " WHERE KeyStreamId= ?";

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
            System.out.println("Deleting entry from KeyInformation table failed!" + "\n");
            e.printStackTrace();
            return false;
        }

    }

    /** Get a new KeyInformationObject by the corresponding keyStreamID
     *
     * @param keyStreamID identifier of the key that needs to be wrapped in a KeyStoreObject
     * @return a new KeyInformationObject containing all the KeyInformation from the Entry with corresponding KeyStreamId
     */
    public static KeyInformationObject getEntryFromKeyInformation(String keyStreamID) {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + infoTableName + " WHERE KeyStreamId = " + "\"" + keyStreamID + "\"";

            Statement stmnt = conn.createStatement();

            ResultSet rs = stmnt.executeQuery(sql);

            KeyInformationObject object = new KeyInformationObject(rs.getString("KeyStreamId"),
                    rs.getInt("KeyBuffer"), rs.getInt("Index_"),
                    rs.getString("Source_"), rs.getString("Destination"));

            stmnt.close();
            conn.close();
            System.out.println("Selecting entry from KeyInformation table was successful!" + "\n");
            return object;


        } catch (SQLException e) {
            System.out.println("Selecting entry from KeyInformation table failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }

    /** Stores all the Entries of the KeyInformation table in an ArrayList and returns the List
     *
     * @return a ArrayList of KeyInformationObject which contain information about the keys currently in storage
     */
    public static ArrayList<KeyInformationObject> getKeyInformationAsList() {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + infoTableName;
            PreparedStatement stmnt = conn.prepareStatement(sql);
            ResultSet rs = stmnt.executeQuery();
            ArrayList<KeyInformationObject> result = new ArrayList<>();
            while(rs.next()) {
                KeyInformationObject res = new KeyInformationObject(rs.getString("KeyStreamID"),
                        rs.getInt("KeyBuffer"), rs.getInt("Index_"), rs.getString("Source_"), rs.getString("Destination"));
                result.add(res);
            }
            stmnt.close();
            conn.close();
            System.out.println("Generating list of KeyInformation table entries was successful" + "\n");
            return result;
        } catch (Exception e) {
            System.out.println("Generating list of all entries from KeyInformation table failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  ---------------------- Methods for the KeyStore Table --------------------------------
     */


    /**Following the Etsi paper there should be 2 Tables. #1 which holds all the information about a key
     * and #2 which stores the real keys.
     *
     * ---> Method inserts a new Entry in table #2 (KeyStore)
     *
     * @param key the real key that needs to be stored
     * @param used Integer indicator whether a key has been used. 0 = False(Not used),  1 = True(Used)
     * @param keyStreamID the reference ID by which the real key can be located
     * @return True if operation was successful, False if it wasn't.
     */
    public static boolean insertToKeyStore(String key, int used, String keyStreamID ){
        try {
            Connection conn = connect();
            String sql = "INSERT INTO KeyStore(Key ,Used, KeyStreamID)  VALUES(?,?,?)";

            PreparedStatement pstmnt = conn.prepareStatement(sql);
            pstmnt.setString(1, key);
            pstmnt.setInt(2, used);
            pstmnt.setString(3, keyStreamID);

            pstmnt.executeUpdate();
            System.out.println("Insertion to KeyStore table was successful");
            pstmnt.close();
            conn.close();

            return true;

        }  catch (SQLException e ){
            System.out.println("Insertion to KeyStore table failed!" + "\n" );
            e.printStackTrace();
        }
        return false;
    }

    /** Change the "Used" parameter of a KeyStore Entry from unused(=0) to used(=1)
     *
     * @param keyStreamID reference ID for a Key
     * @return true if operation succeeded, false otherwise
     */
    public static boolean changeKeyToUsed(String keyStreamID){
        try {
            Connection conn = connect();

            String sql = "UPDATE " + keyTableName + " SET Used = 1 WHERE KeyStreamID = ?";
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            pstmnt.executeUpdate();
            System.out.println("Changed key from Unused to Used");
            pstmnt.close();
            conn.close();

            return true;

        } catch (SQLException e) {
            System.out.println("Changing the used Parameter of Entry failed " + "\n");
            e.printStackTrace();
            return false;
        }
    }

    /** Deletes the real key in the KeyStore by a given keyStreamID
     *
     * @param keyStreamID the ID of the key that needs to be deleted from the DB
     * @return True if operation was successful, False otherwise
     */
    static boolean deleteKeyByID(String keyStreamID){

        try {
            String sql = "DELETE FROM " + keyTableName + " WHERE KeyStreamId= ?";

            Connection conn = connect();
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            pstmnt.executeUpdate();
            System.out.println("Key was deleted successfully!");
            pstmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e) {
            System.out.println("Key deletion failed!" + "\n");
            e.printStackTrace();
            return false;
        }

    }


    /** Get a new KeyStoreObject by the corresponding keyStreamID
     *
     * @param keyStreamID identifier of the key that needs to be wrapped in a KeyStoreObject
     * @return a new KeyStoreObject containing the real key, used parameter and the keyStreamID from the Entry with corresponding KeyStreamId
     */
    public static KeyStoreObject getEntryFromKeyStore(String keyStreamID) {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + keyTableName + " WHERE KeyStreamId = " + "\"" + keyStreamID + "\"";

            Statement stmnt = conn.createStatement();

            ResultSet rs = stmnt.executeQuery(sql);

            KeyStoreObject object = new KeyStoreObject(rs.getString("Key"),
                    rs.getInt("Used"),rs.getString("KeyStreamId"));

            stmnt.close();
            conn.close();
            System.out.println("Selecting entry from KeyStore table was successful!" + "\n");
            return object;


        } catch (SQLException e) {
            System.out.println("Selecting the entry failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }


    /** Stores all the Entrys of the KeyStore Table in an ArrayList and returns the List
     *
     * @return a ArrayList of KeyStoreObjects which contain information about the keys currently in storage
     */
    public static ArrayList<KeyStoreObject> getKeyStoreEntriesAsList() {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + keyTableName;
            PreparedStatement stmnt = conn.prepareStatement(sql);
            ResultSet rs = stmnt.executeQuery();
            ArrayList<KeyStoreObject> result = new ArrayList<>();
            while(rs.next()) {
                KeyStoreObject object = new KeyStoreObject(rs.getString("Key"),
                        rs.getInt("Used"),rs.getString("KeyStreamId"));
            }
            stmnt.close();
            conn.close();
            System.out.println("Generating a list of all entries from KeyStore table succeeded!" + "\n");
            return result;
        } catch (Exception e) {
            System.out.println("Generating a list of all KeyStore table entries failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }


    /**
     *  ------ API Methods ------
     */

    /** from etsi paper: Reserve an association (Key_stream_ID) for a set of future keys at both ends of the QKD link
     through this distributed QKD key management layer and establish a set of parameters that
     define the expected levels of key service. This function shall block until the peers are
     connected or until the Timeout value in the QoS parameter is exceeded.
     *
     * @param source identifier for the source application
     * @param destination identifier for the destination application
     * @param Qos                                               //Weiß selber ned wie der Parameter aussehen soll
     * @param keyStreamID reference ID to locate a key
     * @param status current state of the request
     * @return number of status
     */
    public int open_Connect(String source, String destination, String Qos, String keyStreamID, int status){
        return 1;
    }

    /** from etsi: Terminate the association established for this Key_stream_ID. No further keys shall be
     allocated for this Key_stream_ID after the association has been closed. Due to timing
     differences at the other end of the link this peer operation will happen at some other time and
     any unused keys shall be held until that occurs and then discarded or the TTL (Time To Live
     QoS parameter) has expired.
     *
     * @param keyStreamID reference ID to locate a key
     * @param status current state of the request
     * @return number of status
     */
    public int close(String keyStreamID, int status){
        //delete keyInformation for this ID
        deleteKeyInformationByID(keyStreamID);
        return 1;
    }

    /** Etsi paper page 9 latest version for more information
     *
     * @param keyStreamID reference ID to locate a key
     * @param status current state of the request
     * @return number of status + the key
     */
    public Map.Entry<String, Integer> get_Key(String keyStreamID, int status) {


        return new AbstractMap.SimpleEntry<String, Integer>("der_herauszugebenede_key", 1);
    }




}

