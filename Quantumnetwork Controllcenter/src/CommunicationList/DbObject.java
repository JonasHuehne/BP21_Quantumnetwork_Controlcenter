package CommunicationList;

public final class DbObject {
    private final String name;
    private final String ipAddr;
    private final int port;

    public DbObject (final String n, final String i, final int p) {
    /**
     * constructor for DbObject, sets all the final variables
     * @param n the name of the entry as string
     * @param i the ip address of the entry as string
     * @param p the port of the entry as int
     */
        name = n;
        ipAddr = i;
        port = p;
    }

    /**
     * getter method for the name
     * @return the name of the db entry as string
     */
    public String getName() {
        return name;
    }

    /**
     * getter method for the ip address
     * @return the ip address of the db entry as string
     */
    public String getIpAddr() {
        return ipAddr;
    }

    /**
     * getter method for the port
     * @return the port of the db entry as int
     */
    public int getPort() {
        return port;
    }
}
