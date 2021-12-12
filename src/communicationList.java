import java.sql.*;

public class communicationList {

    private static Connection connection;
    private static String dbPath = "testDb"; //TODO: where should the db be located? changeable?
    enum StmtType {
        INSERT,
        UPDATE,
        DELETE,
        QUERY
    }

    //TODO: (static) check for JDBC driver?
    //TODO: access check (or something similar) for security?

    // open connection to db
    private static void connectToDb() {
        if(connection == null) {
            try{
                connection = DriverManager.getConnection(dbPath);
            } catch (Exception e) {
                // TODO: error handling
            }
        }
    }

    // insert a new entry in the db
    //TODO: takes/returns; Several or just one? Problems with several accesses or already handled?
    public static void dbOperation(StmtType type) {
        connectToDb();
        try {
            Statement statement = connection.createStatement();
            String sql;
            switch(type) {
                case INSERT:
                    sql = "INSERT;";
                    break;
                case UPDATE:
                    sql = "UPDATE;";
                    break;
                case QUERY:
                    sql = "SELECT;";
                    break;
                case DELETE:
                    sql = "DELETE;";
                    break;
                default:
                    //TODO: what should the default case be?
                    sql = "";
                    break;
            }
            statement.executeUpdate(sql);
            connection.close();
        } catch (Exception e) {
            // TODO: error handling
        }
    }

}
