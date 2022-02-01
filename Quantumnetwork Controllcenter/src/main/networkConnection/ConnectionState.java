package main.networkConnection;
/**
 * Represents the State of a single ConnectionEndpoint.
 * @author Jonas Huehne
 *
 */
public enum ConnectionState {
	CLOSED,	//This is the initial state of a ConnectionEndpoint. This means there is no connection being created or used right now.
	CONNECTED,	//This represents that the ConnectionEndpoint is connected with another CE and is ready to send messages.
	CONNECTING,	//This is the state the CE is in during the creation of a connection
	WAITINGFORCONNECTION,	//This implies the CE is waiting for a connection attempt from an external CE and is not currently connected or creating a connection.
	WAITINGFORMESSAGE,	//This is the state if the CE is not connected to anything but still waiting for a message from the outside.
	ERROR	//This is a catch-all fallback. It is the active state if no other valid state can be determined or if the state of a non-existing CE was checked.
}
