package networkConnection;
/**
 * Represents the State of a single ConnectionEndpoint.
 * @author Jonas Huehne
 *
 */
public enum ConnectionState {
	CLOSED,
	CONNECTED,
	CONNECTING,
	WAITINGFORCONNECTION,
	WAITINGFORMESSAGE,
	ERROR
}
