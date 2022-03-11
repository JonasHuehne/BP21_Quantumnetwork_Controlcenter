package networkConnection;

import java.io.Serializable;

/**A wrapper for a String Transmission that includes a head String that is used to identify the transmission type/purpose.
 * 
 * @author Jonas Huehne
 *
 */
public class NetworkPackage implements Serializable{

	private static final long serialVersionUID = -6406450845229886762L;
	private TransmissionTypeEnum head;
	private String typeArgument; 
	private byte[] content;
	private byte[] signature;
	
	/**Supply the newly created NetworkPackage with a TransmissionType, an Argument depending on the type and the actual content of the package.
	 * 
	 * @param head	the type of the transmission
	 * @param typeArgument	additional argument depending on the transmission type
	 * @param content	the actual content of the transmission
	 * @param sig	the signature if the NetworkPackage is used for authenticated communication
	 */
	public NetworkPackage(TransmissionTypeEnum head, String typeArgument, byte[] content, byte[] sig) {
		this.head = head;
		this.typeArgument = typeArgument;
		this.content = content;
		this.signature = sig;
	}
	
	/**Returns the type of this NetworkPackage.
	 * 
	 * @return the TransmissionTypeEnum that describes this NetworkPackages type.
	 */
	public TransmissionTypeEnum getHead() {
		return head;
	}
	
	/**Returns the argument of this transmission, may be "" depending on the transmissionType.
	 * 
	 * @return the argument String
	 */
	public String getTypeArg() {
		return typeArgument;
	}
	
	/**Returns the content of the transmission. May be "".
	 * 
	 * @return the transmissions content String.
	 */
	public byte[] getContent() {
		return content;
	}
	
	/**Returns the signature of the transmission. May be "" for non-authenticated messages.
	 * 
	 * @return the signature of the transmission.
	 */
	public byte[] getSignature() {
		return signature;
	}
	
}
