package networkConnection;
/**
 * Represents the State of a single ConnectionEndpoint.
 * @author J-man
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
