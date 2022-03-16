package networkConnection;

/**This Enum is used to indicate what type of transmission is being sent/received.
 * These Values are used to control the automatic processing of received messages in connectionEndpoint.processMessage().
 * No further manual processing is necessary.
 * New types of messages should be registered here as a new EnumValue and the appropriated processing should be added to connectionEndpoint.processMessage().
 * @author Jonas Huehne, Sasha Petri
 *
 */
public enum TransmissionTypeEnum {
	/** Indicates that the transmission sent is a request for connection establishment. */
	CONNECTION_REQUEST,
	/** Transmission of this type are a confirmation to a {@link #CONNECTION_REQUEST} - they indicate that the CE that received the request has accepted it. */
	CONNECTION_CONFIRMATION,	
	/** This type of transmission indicates the sender will close the connection. Used to inform the receiver so they can also close their connection. */
	CONNECTION_TERMINATION,	//This type of transmission signals that the connection is going to be closed from the senders end and the receiver should do the same.
	/** Transmission type for simple text (string) messages. */
	TEXT_MESSAGE,
	/** Transmission type for the transfer of files rather than text messages. 
	 * @implNote The {@code typeArgument} of the {@linkplain NetworkPackage} is expected to contain the metadata about the file.*/ 
	FILE_TRANSFER,
	/** Confirms that a message was received. Messages of this type contain the messageID of the received message as their content. */
	RECEPTION_CONFIRMATION,
	/** This is sent to another {@linkplain ConnectionEndpoint} to ask if that CE wants to generate a common key for encrypted communication. */
	KEYGEN_SYNC_REQUEST,
	/** This is a positive response to KEYGEN_SYNC_REQUEST and will continue the key-generation process. */
	KEYGEN_SYNC_ACCEPT,	
	/** This is a negative response to KEYGEN_SYNC_REQUEST and will abort the key-generation process. */
	KEYGEN_SYNC_REJECT,
	/** This is used for data transmission during the Key Generation. */
	KEYGEN_TRANSMISSION,
	/** This is used for signals intended for the Photon source. The transmissions contents will be written into a .txt at the Source Servers location. */
	KEYGEN_SOURCE_SIGNAL,
	/** Signals the recipient that the sender wants to stop the key-generation process. */
	KEYGEN_TERMINATION,
	/** Transmissions of this type are used by party A to indicate to party B, that A wishes to use bytes of their shared key. 
	 * This allows B to adjust their own key index accordingly. Transmissions of this type are expected to have an argument
	 * specifying at which key index A wants to start using bytes, and should be signed (to avoid third-party interference). 
	 * B is expected to send back a {@link #RECEPTION_CONFIRMATION} to say "ok" to the key use. */
	KEY_USE_ALERT,
	/**
	 * Transmissions of this type are one of the two answers to {@linkplain #KEY_USE_ALERT}.
	 * By sending this message to A, B tells A that they have agreed to A using the next n bytes at the key index A asked about.
	 * The content of messages of this type is expected to be the ID of the {@link #KEY_USE_ALERT} they are accepting.
	 * Transmissions of this type are expected to be signed.
	 */
	KEY_USE_ACCEPT,
	/**
	 * Transmissions of this type are one of the two answers to {@linkplain #KEY_USE_ALERT}.
	 * By sending this message to A, B tells A that there was a problem with A using the next n bytes at the key index A asked about.
	 * Transmissions of this type will have their key index argument set to B's current key index to allow A to re-synchronize their index.
	 * The content of messages of this type is expected to be the ID of the {@link #KEY_USE_ALERT} they are rejecting.
	 * Transmissions of this type are expected to be signed.
	 */
	KEY_USE_REJECT;
	; 
}
