package CommunicationList;

public final class DbObject {
    private final String name;
    private final String ipAddr;
    private final int port;

    public DbObject (final String n, final String i, final int p) {
        name = n;
        ipAddr = i;
        port = p;
    }

    public String getName() {
        return name;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getPort() {
        return port;
    }
}
