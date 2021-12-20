
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class KeyStoreDbManager {
    String dataBaseName;
    String tableName;


    /**Ist wohl schlauer das so wie du zu machen Sarah und nicht jedes mal nach dem DB Namen zu fragen... im finalen Projekt wird es ja eh nur eine geben
     * @param dataBaseName
     * @return
     */
    private static Connection connect(String dataBaseName) {
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


    // Creates a new Database in the current project directory folder
    // param: (String) fileName  -> fileName for the new Database

    /**
     *
     * @param dataBaseName
     */
     static boolean createNewDb(String dataBaseName) {
        boolean bool = false;

        try {
            Connection conn = KeyStoreDbManager.connect(dataBaseName);//DriverManager.getConnection(path);

            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();

                //System.out.println("Driver Name is -> " + meta.getDriverName());
                System.out.println("Database was created successfully!");


            }
            bool = true;

        } catch (SQLException e) {
            System.out.println("Database creation failed!" + "\n");
            e.printStackTrace();

        }
        return bool;
    }

    /**
     *  @param dataBaseName
     * @param tableName
     * @return
     */
    static boolean createNewTable(String dataBaseName, String tableName){
        boolean bool = false;

        try {
            Connection conn = connect(dataBaseName);//DriverManager.getConnection(url);
            Statement stmnt = conn.createStatement();

            //SQL Statement to create new Table
            String newTableSql  = "CREATE TABLE IF NOT EXISTS " + tableName +
                    " (KeyStreamId CHAR(128) NOT NULL," +
                    " KeyBuffer INTEGER NOT NULL, " +
                    " Index_ INTEGER NOT NULL, " +
                    " Source_ TEXT NOT NULL, " +
                    " Destination TEXT NOT NULL, " +
                    "PRIMARY KEY (KeyStreamId, Index_))";

            stmnt.executeUpdate(newTableSql);
            System.out.println("Table creation successful!");
            stmnt.close();
            conn.close();

            bool = true;


        } catch (SQLException e) {
            System.out.println("Table creation failed!" + "\n" );
            e.printStackTrace();
        }
        return bool;
    }

    /**
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyBuffer array of char bytes ()
     * @param index
     * @param source
     * @param destination
     */
     static boolean insertToDb( String dataBasename, String keyStreamID, int keyBuffer, int index, String source, String destination ){
        boolean bool = false;
        try{


            Connection conn = connect(dataBasename);
            //stmnt = conn.createStatement();

            //wollte den sql String eig private machen
            String sql = "INSERT INTO KeyInformations(keyStreamID, keyBuffer, index_, source_, destination)  VALUES(?,?,?,?,?)";

            //"VALUES(" + "\"" + keyStreamID + "\"" +", \""+ keyBuffer + "\" , \"" +index + "\" , \"" + source + "\", \"" + destination +"\" )";

            //stmnt.executeUpdate(sql);

            PreparedStatement prepStmnt = conn.prepareStatement(sql);

            prepStmnt.setString(1, keyStreamID);
            prepStmnt.setInt(2, keyBuffer);
            prepStmnt.setInt(3, index);
            prepStmnt.setString(4, source);
            prepStmnt.setString(5, destination);

            prepStmnt.executeUpdate();
            System.out.println("Insertion to DB was successful");

            //stmnt.close();
            //conn.commit();
            prepStmnt.close();
            conn.close();

            bool = true;
        }

        catch (SQLException e ){
            System.out.println("Inserion to DB failed!" + "\n" );
            e.printStackTrace();
        }
        return bool;
    }

    /**
     *
     * @param dataBasename
     * @param tableName
     */
     static void selectAll(String dataBasename, String tableName){

        try {
            String sql = "SELECT * FROM " + tableName;
            Connection conn = connect(dataBasename);
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
        } catch (SQLException e) {
            System.out.println( "Selecting Everything from DB= " + dataBasename + "and Table=" + tableName + "\n" );
            e.printStackTrace();
        }
    }

     static boolean deleteEntryByID(String databaseName, String tableName, String keyStreamID){
         boolean bool = false;
        try {
            String sql = "DELETE FROM " + tableName + " WHERE KeyStreamId= ?";

            Connection conn = connect(databaseName);
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            //stmnt.executeQuery(sql);
            pstmnt.executeUpdate();
            System.out.println("Entry was deleted successfully!");
            pstmnt.close();
            conn.close();

            bool = true;
        }

        catch (SQLException e) {
            System.out.println("Entry deletion failed!" + "\n");
            e.printStackTrace();
        }
        return bool;
    }


    /** Nur für Testzwecke
     *
     * @param sqlQuery
     * @param dataBaseName
     */
    public static void executeQuery(String sqlQuery, String dataBaseName){
        try {
            Connection conn = connect(dataBaseName);
            Statement stmnt = conn.createStatement();
            stmnt.executeUpdate(sqlQuery);

            System.out.println("Query was Successful!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static ArrayList<KeyStoreObject> getEntrysAsList (String dataBaseName, String tableName) {
        try {
            //if (connection == null || connection.isClosed()) {
                Connection conn = connect(dataBaseName);
            //}
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
            System.err.println("Problem with query for data in CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }


}

