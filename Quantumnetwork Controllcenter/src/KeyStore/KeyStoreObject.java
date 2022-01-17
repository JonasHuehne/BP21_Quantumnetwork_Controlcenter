package KeyStore;

public class KeyStoreObject {
    /**
     * This Class represents an KeyStore Object which holds the real key
     */

    private final String key;
    private final int used; // 0=False, 1=True
    private final String keyStreamID;

    public KeyStoreObject(String key, int used, String keyStreamID) {
        this.key = key;
        this.used = used;
        this.keyStreamID = keyStreamID;

    }

    /**
     *
     * @return the key from an KeyStore Object
     */
    public String getKey(){return key;}

    /**
     *
     * @return the used parameter which indicates if a key has already been used
     */
    public int getUsed(){return used;}

    /**
     *
     * @return the keyStreamID of an KeyStore Object
     */
    public String getKeyStreamID(){return keyStreamID;}

}

