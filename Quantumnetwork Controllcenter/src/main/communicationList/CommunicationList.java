package communicationList;

import java.util.ArrayList;

/**
 * Interface for interacting with the Communication List
 * @author Sarah Schumann
 */
public interface CommunicationList {

    /**
     * Method to insert an entry in the communication list
     * @param name the name of the contact partner as String
     * @param ipAddress the ip address of the contact partner as String
     * @param port the port of the contact partner as int
     * @param signatureKey the signature public key of the contact partner as String
     *                     (can be "" or null and added later)
     * @return true if it worked, false if error or illegal input
     */
    boolean insert(final String name, final String ipAddress, final int port, final String signatureKey);

    /**
     * Method to delete an entry from the communication list
     * @param name the name of the entry to be deleted as String
     * @return true if it worked or not there, false if error
     */
    boolean delete (final String name);

    /**
     * Method to change the name of an entry
     * @param oldName the former name to replace as String
     * @param newName the new name as String
     * @return true if it worked, false if error or illegal input
     */
    boolean updateName (final String oldName, final String newName);

    /**
     * Method to change the IP address of an entry
     * @param name the name ot the entry to be changed as String
     * @param ipAddress the new IP address as String
     * @return true if it worked, false if error or illegal input
     */
    boolean updateIP (final String name, final String ipAddress);

    /**
     * Method to change the port of an entry
     * @param name the name ot the entry to be changed as String
     * @param port the new port as int
     * @return true if it worked, false if error or illegal input
     */
    boolean updatePort (final String name, final int port);

    /**
     * Method to change the signature key of an entry
     * @param name the name ot the entry to be changed as String
     * @param signatureKey the new signature public key as String
     * @return true if it worked, false if error
     */
    boolean updateSignatureKey (final String name, final String signatureKey);

    /**
     * Method to get an entry from the communication list by name
     * @param name the name of the entry to be returned as String
     * @return the entry as a DbObject
     */
    Contact query (final String name);

    /**
     * Method to get an entry from the communication list by IP address and port
     * @param ipAddress the IP address of the entry to be returned as String
     * @param port the port of the entry to be returned as int
     * @return the entry as a DbObject
     */
    Contact query (final String ipAddress, final int port);

    /**
     * Method to all entries from the communication list
     * @return an ArrayList of DbObjects for the entries
     */
    ArrayList<Contact> queryAll ();

}
