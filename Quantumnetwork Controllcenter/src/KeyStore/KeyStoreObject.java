package KeyStore;

/**
 *   Class represents a KeyInformationObject Object.
 *   Containing all the necessary Information about a certain key
 */

public final class KeyStoreObject {

    private final String keyStreamID;
        private final byte[] keyBuffer;
        private final int index;
        private final String source;
        private final String destination;
        private final boolean used;

        public KeyStoreObject(final String keyStreamID, final byte[] keyBuffer, final  int index, final  String source, final String destination, boolean used) {
            this.keyStreamID = keyStreamID;
            this.keyBuffer = keyBuffer;
            this.index = index;
            this.source = source;
            this.destination = destination;
            this.used = used;

        }

    /**
     *
     * @return KeyStreamID of the entry
     */
    protected String getID() {
            return keyStreamID;
        }

    /**
     *
     * @return keyBuffer (=key) of the entry
     */
        protected byte[] getBuffer() {
            return keyBuffer;
        }

    /**
     *
     * @return Index of entry
     */
        protected int getIndex(){
            return index;
        }

    /**
     *
     * @return Source of the entry
     */
        protected String getSource(){
            return source;
        }

    /**
     *
     * @return Destination of the entry
     */
        protected String getDestination(){
            return destination;
        }

    /**
     *
     * @return boolean parameter indicating whether this key has been used already
     */
    protected boolean getUsed(){return used;}
    }



