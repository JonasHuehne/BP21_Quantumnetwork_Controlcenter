package CommunicationList;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class Database {

    private static Connection connection;

    private static final String tableName = "CommunicationList";

    /**
     * open a connection to the db
     * @return true if it worked, false if error
     */
    private static boolean connectToDb() {
        try{
            Class.forName("org.sqlite.JDBC");

            String dirPath = System.getProperty("user.dir");
            String dbPath = "jdbc:sqlite:" + dirPath + File.separator + "CommunicationList.db";
            connection = DriverManager.getConnection(dbPath);

            Statement stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS " + tableName
                    + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "Name VARCHAR(255) UNIQUE, IPAddress VARCHAR(255),  "
                    + "Port INTEGER);";
            stmt.executeUpdate(sql);
            return true;
        } catch (Exception e) {
            System.err.println("Problem connecting to the CommunicationList Database(" + e.getMessage() + ")");
            return false;
        }
    }

    public static boolean insert (String name, String ipAddr, int port) {
    /**
     * insert a new entry into the db
     * @param name the designated name of the communication partner as a string
     * @param ipAddress the ip address of the communication partner as a string
     * @param port the port as an int
     * @return true if the insert worked, false if error
     */
        try {
            if (connection == null || connection.isClosed()) {
                if(!connectToDb()) {
                    return false;
                }
            }
            String sql = "INSERT INTO " + tableName + "(Name, IPAddress, Port) VALUES(?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, ipAddress);
            stmt.setInt(3, port);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with inserting data in the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * delete an entry from the db
     * @param name the designated name of the entry to be deleted as a String
     * @return true if the deleting worked, false if error
     */
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
            System.err.println("Problem with deleting data from the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * update the designated name of an entry in the db
     * @param oldName the former name as a string
     * @param newName the new name as a string
     * @return true if the update worked, false if error
     */
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
            System.err.println("Problem with updating data in the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * update the IP address in an entry from the db
     * @param name the designated name of the entry to be updated as string
     * @param ipAddress the new IP address as a string
     * @return true if the update worked, false if error
     */
    public static boolean updateIP (String name, String ipAddress) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "UPDATE " + tableName + " SET IPAddress = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, ipAddress);
            stmt.setString(2, name);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with updating data in the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * update the port of an entry from the db
     * @param name the designated name of the entry to be updated as string
     * @param port the new port as an int
     * @return true if the update worked, false if error
     */
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
            System.err.println("Problem with updating data in the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * query one entry from the db by name
     * @param name the designated name of the entry to return as string
     * @return a DbObject with the date of the entry, null if error
     */
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
            System.err.println("Problem with query for data in the CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * query all entries in the db
     * @return ArrayList of DbObjects for all entries in the database, null if error
     */
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
            System.err.println("Problem with query for data in the CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }

}
