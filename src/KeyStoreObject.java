public final class KeyStoreObject {


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

        public String getID() {
            return keyStreamID;
        }

        public int getBuffer() {
            return keyBuffer;
        }

        public int getIndex(){
            return index;
        }

        public String getSource(){
            return source;
        }

        public String getDestination(){
            return destination;
        }

    }

