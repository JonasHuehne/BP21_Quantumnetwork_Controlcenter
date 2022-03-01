package networkConnection;

/**This Enum is used to indicate what type of transmission is being sent/received.
 * These Values are used to control the automatic processing of received messages in connectionEndpoint.processMessage().
 * No further manual processing is necessary.
 * New types of messages should be registered here as a new EnumValue and the appropriated processing should be added to connectionEndpoint.processMessage().
 * @author Jonas Huehne
 *
 */
public enum TransmissionTypeEnum {
	CONNECTION_REQUEST,	//A ConnectionRequest is sent if establishConnection() is called, to create a connection from one connectionEndpoint to another.
	CONNECTION_TERMINATION,	//This type of transmission signals that the connection is going to be closed from the senders end and the receiver should do the same.
	TRANSMISSION,	//This is the default transmission type, used to transmit anything that has "actual content", like a file or text message via the NetworkPackage.content String field.
	FILE_TRANSFER,	//
	RECEPTION_CONFIRMATION_REQUEST,	//This is a regular TRANSMISSION, but also keeps track of sent messages and waits for the recipient of the message to respond, to confirm the arrival of the message.
	RECEPTION_CONFIRMATION_RESPONSE,	//This is what the recipient of a RECEPTION_CONFIRMATION_REQUEST-Transmission uses to confirm the reception.
	KEYGEN_SYNC_REQUEST,	//This is sent to another connectionEndpoint to ask if that cE wants to generate a common key for encrypted communication.
	KEYGEN_SYNC_ACCEPT,	//This is a positive response to KEYGEN_SYNC_REQUEST and will continue the key-generation process.
	KEYGEN_SYNC_REJECT,	//This is a negative response to KEYGEN_SYNC_REQUEST and will abort the key-generation process.
	KEYGEN_TERMINATION;	//This causes the recipient to stop the key-generation process.
}
