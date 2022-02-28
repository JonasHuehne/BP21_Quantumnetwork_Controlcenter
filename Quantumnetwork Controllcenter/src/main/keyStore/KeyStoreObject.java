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
        private boolean initiative;

        public KeyStoreObject(final String keyStreamID, final byte[] keyBuffer, final  int index, final  String source, final String destination, boolean used, boolean iniative) {
            this.keyStreamID = keyStreamID;
            this.keyBuffer = keyBuffer;
            this.index = index;
            this.source = source;
            this.destination = destination;
            this.used = used;
            this.initiative = iniative;

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
     * This method returns t
     */

        public byte[] getCompleteKeyBuffer() {
            return keyBuffer;
        }


    /** USE THIS METHOD if a "ready to be used" key needs to be used
     * Index will be updated automatically within this function
     *
     * @param keyLength
     * @return a new byte[] that starts at the correct Index and has size of the desired keyLength.
     */
    public byte[] getKey(int keyLength){
            int startIndex = index;
            int lastindex = keyBuffer.length;

            // return byte[] of keyLength which contains a usable key
            if(KeyStoreDbManager.enoughKeyMaterialLeft(keyStreamID, keyLength)){
                byte[] keyArray = Arrays.copyOfRange(keyBuffer, startIndex, startIndex+ keyLength);
                //update index parameter
                KeyStoreDbManager.changeIndex(keyStreamID, keyLength);
                //update index for the object that the method is called on
                this.index = startIndex + keyLength;
                return keyArray;
            }

            // if not enough keyMaterial is left we'll just return null
            else{

                // Error message will be displayed through changeIndex method + it will be set to used
                KeyStoreDbManager.changeIndex(keyStreamID, lastindex);
                //update index for the object that the method is called on
                this.index = lastindex;
                return null;
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
     * @return boolean initiative parameter
     */
    public boolean getInitiative() {
        return initiative;
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



