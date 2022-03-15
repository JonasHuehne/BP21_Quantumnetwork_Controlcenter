package keyStore;

/**
 * An Object of this type contains the information about a key stored in the keystore databas.e
 * @author Aron Hernandez, Sasha Petri
 */

public final class KeyStoreObject {

	private final String keyStreamID;
	private final byte[] keyBuffer;
	private int index;
	private final String source;
	private final String destination;
	private final boolean used;
	private boolean initiative;

	/**
	 * Constructor.
	 * @param keyStreamID
	 * 		ID of the key
	 * @param keyBuffer
	 * 		the key itself
	 * @param index
	 * 		current index of the key
	 * @param source      
	 * 		identifier for the source application
	 * @param destination 
	 * 		identifier for the destination application
	 * @param used
	 * 		whether the key has been used already
	 * @param iniative
	 * 		true for the one who initiated key generation, false for the other party
	 */
	protected KeyStoreObject(final String keyStreamID, final byte[] keyBuffer, final int index, final String source,
			final String destination, boolean used, boolean iniative) {
		this.keyStreamID = keyStreamID;
		this.keyBuffer = keyBuffer;
		this.index = index;
		this.source = source;
		this.destination = destination;
		this.used = used;
		this.initiative = iniative;

	}

	/**
	 * @return KeyStreamID of the entry
	 */
	public String getID() {
		return keyStreamID;
	}

	/**
	 * @return byte[] keyBuffer (=key) of the entry This method returns t
	 */

	public byte[] getCompleteKeyBuffer() {
		return keyBuffer;
	}

	/**
	 * @return Index of entry
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return boolean initiative parameter
	 */
	public boolean getInitiative() {
		return initiative;
	}

	/**
	 * @return Source of the entry
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @return Destination of the entry
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @return boolean parameter indicating whether this key has been used already
	 */
	public boolean isUsed() {
		return used;
	}
}
