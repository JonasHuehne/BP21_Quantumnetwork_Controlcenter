package main.keyStore;

/**
 * This Class represents a QoS (= Quality of Service) Object. This Class contains many parameters which are essential for the KeyStore
 */

public class QoS {

    private final int keyChunkSize; // Length of the key buffer, in Bytes.
    private final int max_bps; // Maximum key rate, in bps.
    private final int min_bps; // Minimum key rate, in bps.
    private final int jitter; // Maximum expected deviation, in bps.
    private final int priority; // Priority of the request.
    private final int timeout; // Time, in msec, after which the call will be aborted, returning an error.
    private final int timeToLive;
    private final char[] metadataMimetype; //The mimetype of the metadata to be delivered by the KM on each GET call.

    public QoS(int keyChunkSize, int max_bps, int min_bps, int jitter, int priority, int timeout, int timeToLive, char[] metadataMimetype) {
        this.keyChunkSize = keyChunkSize;
        this.max_bps = max_bps;
        this.min_bps = min_bps;
        this.jitter = jitter;
        this.priority = priority;
        this.timeout = timeout;
        this.timeToLive = timeToLive;
        this.metadataMimetype = metadataMimetype;
    }

    /**
     *
     * @return
     */
    public int getKeyChunkSize() {
        return keyChunkSize;
    }

    /**
     *
     * @return
     */
    public int getMax_bps() {
        return max_bps;
    }

    /**
     *
     * @return
     */
    public int getMin_bps() {
        return min_bps;
    }

    /**
     *
     * @return
     */
    public int getJitter() {
        return jitter;
    }

    /**
     *
     * @return
     */
    public int getPriority() {
        return priority;
    }

    /**
     *
     * @return
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     *
     * @return
     */
    public int getTimeToLive() {
        return timeToLive;
    }

    /**
     *
     * @return
     */
    public char[] getMetadataMimetype() {
        return metadataMimetype;
    }
}
