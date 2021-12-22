package CommunicationList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class Database {

    private static Connection connection;

    private static final String tableName = "CommunicationList";

    // open connection to db
    private static boolean connectToDb() {
        try{
            Class.forName("org.sqlite.JDBC");

            String dirPath = System.getProperty("user.dir");
            String dbPath = "jdbc:sqlite:" + dirPath + "/CommunicationList.db";
            connection = DriverManager.getConnection(dbPath);

            Statement stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName
                    + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "Name VARCHAR(255) UNIQUE, IPAddress VARCHAR(255),  "
                    + "Port INTEGER);";
            stmt.executeUpdate(sql);
            return true;
        } catch (Exception e) {
            System.err.println("Problem connecting to CommunicationList Database(" + e.getMessage() + ")");
            return false;
        }
    }

    // insert a new entry in the db
    // input: Name (String), IP Address (String), Port (int)
    // output: true, if the insert worked
    public static boolean insert (String name, String ipAddr, int port) {
        try {
            if (connection == null || connection.isClosed()) {
                if(!connectToDb()) {
                    return false;
                }
            }
            String sql = "INSERT INTO " + tableName + "(Name, IPAddress, Port) VALUES(?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, ipAddr);
            stmt.setInt(3, port);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with inserting data in CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    // delete an entry from the db
    // input: Name (String) of the entry to delete
    // output: true, if the delete worked
    public static boolean delete (String name) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "DELETE FROM " + tableName + " WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with deleting data from CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    // update the name in an entry from the db
    // input: oldName (String) of the entry to update, newName (String)
    // output: true, if the update worked
    public static boolean updateName (String oldName, String newName) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "UPDATE " + tableName + " SET Name = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with updating data in CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    // update the IP address in an entry from the db
    // input: Name (String) of the entry to update, the new IP address (String)
    // output: true, if the update worked
    public static boolean updateIP (String name, String ipAddr) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "UPDATE " + tableName + " SET IPAddress = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, ipAddr);
            stmt.setString(2, name);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with updating data in CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    // update the name in an entry from the db
    // input: Name (String) of the entry to update, the new port (int)
    // output: true, if the update worked
    public static boolean updatePort (String name, int port) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "UPDATE " + tableName + " SET Port = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, port);
            stmt.setString(2, name);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with updating data in CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    // query one entry by name
    // input: Name (String) of the entry to return
    // output: a DbObject with the data; if error or not there: null
    public static DbObject query (String name) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "SELECT Name, IPAddress, Port FROM " + tableName + " WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return new DbObject(rs.getString("Name"),
                    rs.getString("IPAddress"), rs.getInt("Port"));
        } catch (Exception e) {
            System.err.println("Problem with query for data in CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }

    // query all entries in the database
    // input: none
    // output: ArrayList of DbObjects of all entries in the database; if error or none: null
    public static ArrayList<DbObject> queryAll () {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "SELECT Name, IPAddress, Port FROM " + tableName;
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<DbObject> result = new ArrayList<>();
            while(rs.next()) {
                DbObject res = new DbObject(rs.getString("Name"),
                        rs.getString("IPAddress"), rs.getInt("Port"));
                result.add(res);
            }
            return result;
        } catch (Exception e) {
            System.err.println("Problem with query for data in CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }

}
