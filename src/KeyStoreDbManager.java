
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class KeyStoreDbManager {
    String dataBaseName;
    String tableName;


    /**
     * @param dataBaseName
     * @return
     */
    public static Connection connect(String dataBaseName) {
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
    public static void createNewDb(String dataBaseName) {


        try {
            Connection conn = KeyStoreDbManager.connect(dataBaseName);//DriverManager.getConnection(path);

            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();

                //System.out.println("Driver Name is -> " + meta.getDriverName());
                System.out.println("Database was created successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Database creation failed!" + "\n");
            e.printStackTrace();
        }

    }

    /**
     *
     * @param dataBaseName
     * @param tableName
     */
    public static void createNewTable(String dataBaseName, String tableName){

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


        } catch (SQLException e) {
            System.out.println("Table creation failed!" + "\n" );
            e.printStackTrace();
        }

    }

    /**
     *
     * @param keyStreamID reference ID to locate a key
     * @param keyBuffer array of char bytes ()
     * @param index
     * @param source
     * @param destination
     */
    public static void insertToDb( String dataBasename, String keyStreamID, int keyBuffer, int index, String source, String destination ){

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
            conn.close();
        }

        catch (SQLException e ){
            System.out.println("Inserion to DB failed!" + "\n" );
            e.printStackTrace();
        }
    }

    /**
     *
     * @param dataBasename
     * @param tableName
     */
    public static void selectAll(String dataBasename, String tableName){

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

        } catch (SQLException e) {
            System.out.println( "Selecting Everything from DB= " + dataBasename + "and Table=" + tableName + "\n" );
            e.printStackTrace();
        }
    }

    public static void deleteEntryByID(String databaseName, String tableName, String keyStreamID){
        try {
            String sql = "DELETE FROM " + tableName + " WHERE KeyStreamId= ?";

            Connection conn = connect(databaseName);
            PreparedStatement pstmnt = conn.prepareStatement(sql);

            pstmnt.setString(1, keyStreamID);
            //stmnt.executeQuery(sql);
            pstmnt.executeUpdate();
            System.out.println("Entry was deleted successfully!");

        }

        catch (SQLException e) {
            System.out.println("Entry deletion failed!" + "\n");
            e.printStackTrace();
        }
    }

    public static int getNumberOfEntrys(String dataBaseName, String tableName){
        int numberOfRows = -1;
        try {
            Connection conn = connect(dataBaseName);
            Statement stmnt = conn.createStatement();
            String sql = "SELECT COUNT(*) FROM " + tableName + ";";


            stmnt.executeUpdate(sql);



        } catch (SQLException e) {
            e.printStackTrace();
        }
        return numberOfRows;
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


}

