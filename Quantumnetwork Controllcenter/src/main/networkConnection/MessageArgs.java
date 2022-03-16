package networkConnection;

import java.io.Serializable;

/**
 * Used to convey meta-information about the message being sent.

 * @author Sasha Petri
 */
public record MessageArgs(String userName, String fileName, int keyIndex, String localIP, int localPort) implements Serializable {
	
	/**
	 * @param userName
	 * 		local user name, used in connection establishment <br>
	 * 		null if not needed
	 * @param fileName
	 * 		name of file being sent, includes extension <br>
	 * 		null if a file is not being sent
	 * @param keyIndex
	 * 		index of the key in the keystore where the used bits for this encryption start  <br>
	 * 		-1 if the file is not encrypted
	 * @param localIP
	 * 		used in connection establishment <br>
	 * 		null if not needed
	 * @param localPort
	 * 		used in connection establishment <br>
	 * 		-1 if not needed
	 */
	public MessageArgs(String userName, String fileName, int keyIndex, String localIP, int localPort) {
		this.userName = userName;
		this.fileName = fileName;
		this.keyIndex = keyIndex;
		this.localIP = localIP; 
		this.localPort = localPort;
	}
	
	/**
	 * Used for {@linkplain NetworkPackage}s sent during connection establishment,
	 * in the case of responding to a connection request.
	 * @param userName
	 * 		the name you wish the other communication partner to know you as
	 */
	public MessageArgs(String userName) {
		this(userName, null, -1, null, -1);
	}

	/**
	 * Used for {@linkplain NetworkPackage}s sent during connection establishment,
	 * in the case of requesting a connection to be established.
	 * @param userName
	 * 		the name you wish the other communication partner to know you as
	 * @param localIP
	 * 		your IP
	 * @param
	 * 		your local server port
	 */
	public MessageArgs(String userName, String localIP, int localPort) {
		this(userName, null, -1, localIP, localPort);
	}
	
	/**
	 * Used for {@linkplain NetworkPackage}s sent for file transfer.
	 * @param fileName
	 * 		the name of the file being transfered, including extension
	 * @param keyIndex
	 * 		encrypted messages use a mutual key between you and your communication partner <br>
	 * 		this is the index of the first bit being used of that mutual key <br>
	 * 		-1 if this not an encrypted file transfer
	 */
	public MessageArgs(String fileName, int keyIndex) {
		this(null, fileName, keyIndex, null, -1);
	}
	
	/**
	 * Used for {@linkplain NetworkPackage}s sent for encrypted transfer of text.
	 * @param keyIndex
	 * 		encrypted messages use a mutual key between you and your communication partner <br>
	 * 		this is the index of the first bit being used of that mutual key
	 */
	public MessageArgs(int keyIndex) {
		this(null, null, keyIndex, null, -1);
	}
	
	/** 
	 * Used for messages without any special arguments / meta-data, e.g. connection close request.
	 */
	public MessageArgs() {
		this(null, null, -1, null, -1);
	}
}
