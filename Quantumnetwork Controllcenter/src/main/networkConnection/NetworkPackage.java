package networkConnection;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

import messengerSystem.Authentication;

/**A wrapper for a String Transmission that includes a head String that is used to identify the transmission type/purpose.
 * 
 * @author Jonas Huehne, Sasha Petri
 *
 */
public class NetworkPackage implements Serializable{

	private static final long serialVersionUID = -6406450845229886763L;
	/** Transmission type of the Network Package, used when parsing it to identify what to do with the package */
	private TransmissionTypeEnum type;
	/** Content of the package, relevant for data transfer (e.g. text messages, file transfer) */
	private byte[] content;
	/** Signature of the package, used to verify the authenticity and integrity of the package */
	private byte[] signature;
	
	/** meta information about the message being sent */
	private MessageArgs args;
	/** ID for the package */
	private byte[] packageID;
	/** true <==> the recipient is expected to send a package of type {@linkplain TransmissionTypeEnum#CONNECTION_CONFIRMATION} back */
	private boolean expectConfirmation;
	
	
	public NetworkPackage(TransmissionTypeEnum type, MessageArgs args, byte[] content, boolean expectConfirmation) {
		this.type 		= type;
		this.args 		= (args == null) ? new MessageArgs() : args; // to avoid NPE
		this.content 	= (content == null) ? new byte[] {} : content; // to avoid any issues with getTotalData()
		this.packageID 	= new byte[32];
		generatePackageID();
		this.expectConfirmation = expectConfirmation;
	}
	
	/**
	 * Used for messages that have no content, e.g. connection termination requests.
	 * @param type
	 * 		the type of message to send
	 * @param args
	 * 		meta-information about the message, necessary for some types (such as connection requests)
	 * @param expectConfirmation
	 * 		true to tell the receiver to send a confirmation message back <br>
	 * 		false to tell the receiver to not send a confirmation message back
	 */
	public NetworkPackage(TransmissionTypeEnum type, MessageArgs args, boolean expectConfirmation) {
		this(type, args, null, expectConfirmation);
	}
	
	/**
	 * Used for messages that have no content and no relevant message arguments.
	 * @param type
	 * 		the type of message to send
	 * @param expectConfirmation
	 * 		true to tell the receiver to send a confirmation message back <br>
	 * 		false to tell the receiver to not send a confirmation message back	
	 */
	public NetworkPackage(TransmissionTypeEnum type, boolean expectConfirmation) {
		this(type, new MessageArgs(), null, expectConfirmation);
	}
	
	/**
	 * Signs the NetworkPackage. <br>
	 * Signs not only the content, but also the meta-data, specifically: <br>
	 *  - the type {@link #getType()} <br>
	 *  - the arguments {@link #getMessageArgs()} <br>
	 *  - the flag on whether confirmation is expected {@link #expectedToBeConfirmed()} <br>
	 *  - the package id <br>
	 *  Sets the signature, which can be retrieved with {@link #getSignature()}.
	 *  @param auth
	 *  		the authenticator to use
	 */
	public void sign(Authentication auth) {
		/*
		 * For the integrity of a NetworkPackage, not only the content is important,
		 * but also the type, arguments, and whether a confirmation is expected.
		 * All of this needs to be signed so that if they are changed along the way the recipient can spot it.
		 * Unfortunately, we can't have two seperate signatures for the meta data and content (see: replay attack)
		 * so we have to create a new, large array in memory. Because of this it is discouraged
		 * to create especially large NetworkPackages.
		 */
		this.signature = auth.sign(getTotalData());
	}
	
	/**
	 * Verifies this NetworkPackage. <br>
	 * Verifies not only the content, but also the meta-data, specifically: <br>
	 *  - the type {@link #getType()} <br>
	 *  - the arguments {@link #getMessageArgs()} <br>
	 *  - the flag on whether confirmation is expected {@link #expectedToBeConfirmed()} <br>
	 *  - the package id <br>
	 * @param auth
	 * 		the authenticator to use, needs to be the same as was used for the signature
	 * 		for the verification to be successful
	 * @param sender
	 * 		the sender of the message, will be needed to look up the public key
	 * @return
	 * 		true if the signature is valid, false if otherwise
	 */
	public boolean verify(Authentication auth, String sender) {
		return auth.verify(getTotalData(), signature, sender);
	}
	
	/**
	 * @return the total data of the message as a single byte array, used for authentication
	 */
	private byte[] getTotalData() {
		byte[] byteArgs = args.toString().getBytes(StandardCharsets.ISO_8859_1);
		byte[] bytesForType = type.toString().getBytes(StandardCharsets.ISO_8859_1);
		byte byteExpectConfirm = expectConfirmation ? (byte) 1 : (byte) 0;
		
		int metaDataLength = byteArgs.length + bytesForType.length + 1 + packageID.length;
		
		byte[] totalData = new byte[metaDataLength + content.length];
		System.arraycopy(byteArgs, 		0, totalData, 	0, 										byteArgs.length);
		System.arraycopy(bytesForType, 	0, totalData, 	byteArgs.length, 						bytesForType.length);
		System.arraycopy(packageID, 	0, totalData,  	byteArgs.length + bytesForType.length, 	packageID.length);
		totalData[metaDataLength - 1] = byteExpectConfirm;
		System.arraycopy(content, 0, totalData, metaDataLength, content.length);
		
		return totalData;
	}
	
	/**Returns the type of this NetworkPackage.
	 * 
	 * @return the TransmissionTypeEnum that describes this NetworkPackages type.
	 */
	public TransmissionTypeEnum getType() {
		return type;
	}
	
	/**
	 * @return the arguments of this message <br>
	 * for many message types, they will not be relevant
	 */
	public MessageArgs getMessageArgs() {
		return args;
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
	 * @return the signature of the transmission
	 */
	public byte[] getSignature() {
		return signature;
	}
	
	/**
	 * Used to generate an ID for a package.
	 * Not guaranteed to be unique, but mathematically likely to.
	 */
	private void generatePackageID() {
		new Random().nextBytes(packageID);
	}


	/**
	 * @return true if this message is expected to be confirmed by the receiver <br>
	 * 		   false otherwise
	 */
	public boolean expectedToBeConfirmed() {
		return expectConfirmation;
	}


	/**
	 * @return ID of this package
	 */
	public byte[] getID() {
		return packageID;
	}
	
	/**
	 * Used to reduce the size of the message for logging purposes.
	 * Deletes the contents of the message.
	 */
	public void clearContents() {
		this.content = new byte[] {};
	}

	/**
	 * @return the base 64 encoded version of this packages message ID
	 */
	public String getStringID() {
		return Base64.getEncoder().encodeToString(getID());
	}
	
}
