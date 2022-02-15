package keyStore;

import java.util.Arrays;

/**
 *   Class represents a KeyInformationObject Object.
 *   Containing all the necessary Information about a certain key
 * @author Aron Hernandez
 */

public final class KeyStoreObject {

    private final String keyStreamID;
        private final byte[] keyBuffer;
        private int index;
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
    public String getID() {
            return keyStreamID;
        }

    /**
     *
     * @return byte[] keyBuffer (=key) of the entry
     */
        public byte[] getKeyBuffer() {
            return keyBuffer;
        }

    /** USE THIS METHOD if a "ready to be used" key needs to be displayed
     *
     * @return a new byte[] that starts at the correct Index.
     */
    public byte[] getKeyFromStartingIndex(int keyLength){
            int startIndex = index;
            int lastindex = keyBuffer.length;

            // return byte[] of keyLength which contains a usable key
            if(KeyStoreDbManager.enoughKeyMaterialLeft(keyStreamID, keyLength)){
                byte[] correctIndex = Arrays.copyOfRange(keyBuffer, startIndex, startIndex+ keyLength);
                //update index parameter
                KeyStoreDbManager.changeIndex(keyStreamID, keyLength);
                return correctIndex;
            }

            // if not enough  keyMaterial is left we'll just return a new byte[] from the index (which will be < keyLength)
            // --> key will be set to used!
            else{
                // error message should be displayed through method "enoughKeyMaterialLeft
                byte[] result = Arrays.copyOfRange(keyBuffer, startIndex, lastindex);
                KeyStoreDbManager.changeIndex(keyStreamID, keyBuffer.length);
                return result;
            }
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

    /**
     *
     * @return boolean parameter indicating whether this key has been used already
     */
    public boolean getUsed(){return used;}
    }



