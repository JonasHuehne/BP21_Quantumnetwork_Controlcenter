package networkConnection;

public enum ConnectionState {
	Closed,
	Connected,
	Connecting,
	WaitingForConnection,
	WaitingForMessage,
	ERROR
}
