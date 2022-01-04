package networkConnection;
/**
 * Represents the State of a single ConnectionEndpoint.
 * @author Jonas H�hne
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
