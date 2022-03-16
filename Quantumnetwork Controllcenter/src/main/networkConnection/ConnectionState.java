package networkConnection;
/**
 * Represents the State of a single ConnectionEndpoint.
 * @author Jonas Huehne
 *
 */
public enum ConnectionState {
	/** This is the initial state of a ConnectionEndpoint. This means there is no connection being created or used right now. */
	CLOSED,	
	/** This represents that the ConnectionEndpoint is connected with another CE and is ready to send messages. */
	CONNECTED,	
	/** This is the state the CE is in during the creation of a connection */
	CONNECTING,	
	/** This implies the CE is waiting for a connection attempt from an external CE and is not currently connected or creating a connection. */
	WAITINGFORCONNECTION,	
	/**This is the state if the CE is not connected to anything but still waiting for a message from the outside.*/
	WAITINGFORMESSAGE,	
	/**This is a catch-all fallback. It is the active state if no other valid state can be determined or if the state of a non-existing CE was checked.*/
	ERROR	
}
