package communicationList;

/**
 * Class for bundling information about a contact in the CommunicationList.
 * @author Sarah Schumann
 */
public final class Contact {

    // class variables representing the contact
    private final String name;
    private final String ipAddress;
    private final int port;
    private final String signatureKey;

    // number how much of the public key should be in the string representation
    private static final int lengthSignatureKeyToString = 7;

    /**
     * Constructor.
     * @param n the name of the contact
     * @param i the ip address of the contact
     * @param p the port of the contact as int
     * @param s the signature key of the contact
     */
    public Contact(final String n, final String i, final int p, final String s) {
        name = n;
        ipAddress = i;
        port = p;
        signatureKey = s;
    }

    /**
     * @return the name of the contact as string
     */
    public String getName() {
        return name;
    }

    /**
     * @return the ip address of the contact as string
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return the port of the contact as int
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the signature key of the contact as string
     */
    public String getSignatureKey () {
        return signatureKey;
    }

    /**
     * toString method for the Contact class
     * writes a shortened version of the public key if one is set
     * @return a String representation of the specific Contact object
     */
    public String toString () {
        String representation = "Name: " + name + ", IP Address: "
                + ipAddress + ", Port: " + port;
        if (!(signatureKey == null || signatureKey.equals(""))) {
            representation = representation + ", Public Key: "
                    + signatureKey.substring(0, Math.min(lengthSignatureKeyToString, signatureKey.length()))
                    + "...";
        }
        return representation;
    }
}
