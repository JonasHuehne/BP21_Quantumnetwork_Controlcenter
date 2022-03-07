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
	/** Default transmission type for anything with "actual content", like a file or a message. 
	 * @deprecated This type of transmission is discouraged from use - more specific types should be used instead. */
	TRANSMISSION,	//This is the default transmission type, used to transmit anything that has "actual content", like a file or text message via the NetworkPackage.content String field.
	/** Transmission type for simple text (string) messages. */
	TEXT_MESSAGE,
	/** Transmission type for the transfer of files rather than text messages. 
	 * @implNote The {@code typeArgument} of the {@linkplain NetworkPackage} is expected to contain the metadata about the file.*/ 
	/*
	 *  TODO: Make special constructor for file networkpackages that calls a private method that transforms some meta-data strings into the correct typeargs
	 *  and then also add a method that interprets these typeargs.
	 */
	FILE_TRANSFER,	
	/** Requests that, if the transmission is received, a transmission with no content and type {@link #RECEPTION_CONFIRMATION_RESPONSE} is sent back. <br>
	 * Other than that, this is identical to {@link #TRANSMISSION}.
	 * @deprecated See {@link #TRANSMISSION}.  */
	// TODO: This could be handled simply via a boolean in the NetworkPackage class (i.e. boolean true <==> receiver sends back an "i got it")
	RECEPTION_CONFIRMATION_REQUEST,	//This is a regular TRANSMISSION, but also keeps track of sent messages and waits for the recipient of the message to respond, to confirm the arrival of the message.
	/**
	 * Transmissions of this type are the response to a {@link #RECEPTION_CONFIRMATION_REQUEST}. <br>
	 * Their content will not be read, they are simply confirmations.
	 * @deprecated See {@link #TRANSMISSION}.
	 */
	RECEPTION_CONFIRMATION_RESPONSE,	//This is what the recipient of a RECEPTION_CONFIRMATION_REQUEST-Transmission uses to confirm the reception.
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
	KEYGEN_TERMINATION;	
}
