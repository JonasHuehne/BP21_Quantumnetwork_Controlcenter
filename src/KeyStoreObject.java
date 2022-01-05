public final class KeyStoreObject {

    /**
     *   Class represents a KeyStore Object.
     *   Containing all the necessary Information about a certain key
     */


    private final String keyStreamID;
        private final int keyBuffer;
        private final int index;
        private final String source;
        private final String destination;

        public KeyStoreObject(final String keyStreamID, final int keyBuffer,final  int index,final  String source,final String destination) {
            this.keyStreamID = keyStreamID;
            this.keyBuffer = keyBuffer;
            this.index = index;
            this.source = source;
            this.destination = destination;

        }

    /**
     *
     * @return KeyStreamID of the entry
     */
    public String getID() {
            return keyStreamID;
        }

    /**
     *
     * @return Buffer of the entry
     */
        public int getBuffer() {
            return keyBuffer;
        }

    /**
     *
     * @return Index of entry
     */
        public int getIndex(){
            return index;
        }

    /**
     *
     * @return Source of the entry
     */
        public String getSource(){
            return source;
        }

    /**
     *
     * @return Destination of the entry
     */
        public String getDestination(){
            return destination;
        }

    }

