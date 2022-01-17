package CommunicationList;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Class to handle interaction with the communication list db
 * @author Sarah Schumann
 */
public class Database {

    private static Connection connection;

    private static final String TABLE_NAME = "CommunicationList";

    /**
     * Regex for checking the validity of the contact name
     * only allows a-z, 0-9. _ and -
     */
    private static final String CONTACT_NAME_SYNTAX = "(\\w|_|-)*";

    /**
     * Regex for checking the validity of the ip
     * only accepts the pattern x.x.x.x with x between 0 and 255
     */
    private static final String CONTACT_IP_SYNTAX =
            "(([0-1]?\\d{1,2})|([2](([0-4]\\d?)|(5[0-5]))))\\." +
            "(([0-1]?\\d{1,2})|([2](([0-4]\\d?)|(5[0-5]))))\\." +
            "(([0-1]?\\d{1,2})|([2](([0-4]\\d?)|(5[0-5]))))\\." +
            "(([0-1]?\\d{1,2})|([2](([0-4]\\d?)|(5[0-5]))))";

    private static final int MIN_PORT_NUMBER = 0;
    private static final int MAX_PORT_NUMBER = 65535;

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
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                    + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "Name VARCHAR UNIQUE, "
                    + "IPAddress VARCHAR, "
                    + "Port INTEGER, "
                    + "SignatureKey VARCHAR, "
                    + "CONSTRAINT uniqueIpPortPair UNIQUE (IPAddress, Port));";
            stmt.executeUpdate(sql);
            stmt.close();
            return true;
        } catch (Exception e) {
            System.err.println("Problem connecting to the CommunicationList Database(" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * insert a new entry into the db
     * @param name the designated name of the communication partner as a string
     * @param ipAddress the ip address of the communication partner as a string
     * @param port the port as an int
     * @param signatureKey the public signature key as a string
     * @return true if the insert worked, false if error
     */
    public static boolean insert (final String name, final String ipAddress, final int port, final String signatureKey) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            // check for illegal input
            if (!Pattern.matches(CONTACT_NAME_SYNTAX, name)
                    || !Pattern.matches(CONTACT_IP_SYNTAX, ipAddress)
                    || port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
                System.err.println("Problem with inserting data in the CommunicationList Database: " +
                        "Name, IP Address or port violates the constraints");
                return false;
            }
            String sql = "INSERT INTO " + TABLE_NAME + "(Name, IPAddress, Port, SignatureKey) VALUES(?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, ipAddress);
            stmt.setInt(3, port);
            stmt.setString(4, signatureKey);
            stmt.executeUpdate();
            stmt.close();
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
    public static boolean delete (final String name) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "DELETE FROM " + TABLE_NAME + " WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.executeUpdate();
            stmt.close();
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
    public static boolean updateName (final String oldName, final String newName) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            // check for illegal input
            if (!Pattern.matches(CONTACT_NAME_SYNTAX, newName)) {
                System.err.println("Problem with updating data in the CommunicationList Database: " +
                        "Name violates the constraints");
                return false;
            }
            String sql = "UPDATE " + TABLE_NAME + " SET Name = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            stmt.executeUpdate();
            stmt.close();
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
    public static boolean updateIP (final String name, final String ipAddress) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            // check for illegal input
            if (!Pattern.matches(CONTACT_IP_SYNTAX, ipAddress)) {
                System.err.println("Problem with updating data in the CommunicationList Database: " +
                        "IP Address violates the constraints");
                return false;
            }
            String sql = "UPDATE " + TABLE_NAME + " SET IPAddress = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, ipAddress);
            stmt.setString(2, name);
            stmt.executeUpdate();
            stmt.close();
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
    public static boolean updatePort (final String name, final int port) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            // check for illegal input
            if(port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
                System.err.println("Problem with updating data in the CommunicationList Database: " +
                        "Port violates the constraints");
                return false;
            }
            String sql = "UPDATE " + TABLE_NAME + " SET Port = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, port);
            stmt.setString(2, name);
            stmt.executeUpdate();
            stmt.close();
            return true;
        } catch (Exception e) {
            System.err.println("Problem with updating data in the CommunicationList Database (" + e.getMessage() + ")");
            return false;
        }
    }

    /**
     * update the signature key of an entry from the db
     * @param name the designated name of the entry to be updated as string
     * @param signatureKey the new signatureKey as a string
     * @return true if the update worked, false if error
     */
    public static boolean updateSignatureKey (final String name, final String signatureKey) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "UPDATE " + TABLE_NAME + " SET SignatureKey = ? WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, signatureKey);
            stmt.setString(2, name);
            stmt.executeUpdate();
            stmt.close();
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
    public static DbObject query (final String name) {
        try {
            if (connection == null || connection.isClosed()) {
                connectToDb();
            }
            String sql = "SELECT Name, IPAddress, Port, SignatureKey FROM " + TABLE_NAME + " WHERE Name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if(rs == null) {
                return null;
            }
            DbObject result = new DbObject(rs.getString("Name"), rs.getString("IPAddress"),
                    rs.getInt("Port"), rs.getString("SignatureKey"));
            rs.close();
            stmt.close();
            return result;
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
            String sql = "SELECT Name, IPAddress, Port, SignatureKey FROM " + TABLE_NAME;
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<DbObject> result = new ArrayList<>();
            while(rs.next()) {
                DbObject res = new DbObject(rs.getString("Name"), rs.getString("IPAddress"),
                        rs.getInt("Port"), rs.getString("SignatureKey"));
                result.add(res);
            }
            rs.close();
            stmt.close();
            return result;
        } catch (Exception e) {
            System.err.println("Problem with query for data in the CommunicationList Database (" + e.getMessage() + ")");
            return null;
        }
    }

}
