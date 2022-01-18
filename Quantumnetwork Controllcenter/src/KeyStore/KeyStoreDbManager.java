package KeyStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class KeyStoreDbManager {
    private static final String dataBaseName = "KeyStore.db";
    private static final String tableName = "KeyInformations";


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
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName +
                        " (KeyStreamId CHAR(128) ," +
                        " KeyBuffer INTEGER NOT NULL, " + // Index muss definitiv noch angepasst werden, Frage im Treffen heute nach
                        " Index_ INTEGER NOT NULL , " +
                        " Source_ TEXT NOT NULL, " +
                        " Destination TEXT NOT NULL, " +
                        "PRIMARY KEY (KeyStreamId, Index_))";

            stmnt.executeUpdate(sql);
            System.out.println("Table creation successful!");
            stmnt.close();
            conn.close();

            return true;

        } catch (SQLException e) {
            System.out.println("Database creation failed!" + "\n");
            e.printStackTrace();

            return false;
        }

    }


    /** Inserts a new Entry to the DB
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyBuffer array of char bytes ()
     * @param index index for the new Key
     * @param source identifier for the source application
     * @param destination identifier for the destination application
     */
    public static boolean insertToDb(  String keyStreamID, byte[] keyBuffer, int index, String source, String destination ){

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
            System.out.println("Insertion to DB was successful");

            prepStmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e ){
            System.out.println("Insertion to DB failed!" + "\n" );
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
            String sql = "UPDATE " + tableName + " SET KeyStreamId = ? WHERE Index_ = ?";
            PreparedStatement pstmnt = conn.prepareStatement(sql);
            pstmnt.setString(1, newID);
            pstmnt.setInt(2, index);
            pstmnt.executeUpdate();

            pstmnt.close();
            conn.close();
            return true;

        } catch (Exception e) {
            System.err.println("Renaming entry failed!" + "\n");
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
    static boolean deleteEntryByID(String keyStreamID){

        try {
            String sql = "DELETE FROM " + tableName + " WHERE KeyStreamId= ?";

            Connection conn = connect();
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            pstmnt.executeUpdate();
            System.out.println("Entry was deleted successfully!");
            pstmnt.close();
            conn.close();

            return true;
        }

        catch (SQLException e) {
            System.out.println("Entry deletion failed!" + "\n");
            e.printStackTrace();
            return false;
        }

    }

    /** Get a new KeystoreObject by the corresponding keyStreamID
     *
     * @param keyStreamID identifier of the key that needs to be wrapped in a KeyStoreObject
     * @return a new KeyStoreObject containing all the KeyInformations from the Entry with corresponding KeyStreamId
     */
    public static KeyStoreObject getEntry(String keyStreamID) {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + tableName + " WHERE KeyStreamId = " + "\"" + keyStreamID + "\"";

            Statement stmnt = conn.createStatement();

            ResultSet rs = stmnt.executeQuery(sql);

            KeyStoreObject object = new KeyStoreObject(rs.getString("KeyStreamId"),
                    rs.getInt("KeyBuffer"), rs.getInt("Index_"),
                    rs.getString("Source_"), rs.getString("Destination"));

            stmnt.close();
            conn.close();
            return object;


        } catch (SQLException e) {
            System.out.println("Selecting the entry failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }

    /** Stores all the Entrys of the DB in an ArrayList and returns the List
     *
     * @return a ArrayList of KeyStoreObjects which contain information about the keys currently in storage
     */
    public static ArrayList<KeyStoreObject> getEntriesAsList () {
        try {
            Connection conn = connect();

            String sql = "SELECT * FROM " + tableName;
            PreparedStatement stmnt = conn.prepareStatement(sql);
            ResultSet rs = stmnt.executeQuery();
            ArrayList<KeyStoreObject> result = new ArrayList<>();
            while(rs.next()) {
                KeyStoreObject res = new KeyStoreObject(rs.getString("KeyStreamID"),
                        rs.getInt("KeyBuffer"), rs.getInt("Index_"), rs.getString("Source_"), rs.getString("Destination"));
                result.add(res);
            }
            stmnt.close();
            conn.close();
            return result;
        } catch (Exception e) {
            System.out.println("Generating a list of all entries failed!" + "\n");
            e.printStackTrace();
            return null;
        }
    }



}

