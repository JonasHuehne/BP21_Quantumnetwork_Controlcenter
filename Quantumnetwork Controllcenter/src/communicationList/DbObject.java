package communicationList;

/**
 * class for bundling information of a sb entry in the communication list
 */
public final class DbObject {
    private final String name;
    private final String ipAddress;
    private final int port;
    private final String signatureKey;

    /**
     * constructor for DbObject, sets all the final variables
     * @param n the name of the entry as string
     * @param i the ip address of the entry as string
     * @param p the port of the entry as int
     * @param s the signature key of the entry as string
     */
    public DbObject (final String n, final String i, final int p, final String s) {
        name = n;
        ipAddress = i;
        port = p;
        signatureKey = s;
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
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * getter method for the port
     * @return the port of the db entry as int
     */
    public int getPort() {
        return port;
    }

    /**
     * getter method for the signature key
     * @return the signature key of the db entry as string
     */
    public String getSignatureKey () {
        return signatureKey;
    }
}
