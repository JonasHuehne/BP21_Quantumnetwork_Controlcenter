package networkConnection;

/**This Enum expresses the State of a NetworkTimeoutThread
 * 
 * @author Jonas Huehne
 *
 */
public enum NetworkTimerState {
	/** Used to indicate the timer has started, but the timeout duration has not yet elapsed. */
	RUNNING,
	/** Used to indicate the timer was started and the timeout duration has elapsed. The timer will have called the method it was supplied. */
	TIMED_OUT,
	/** Used to indicate the timer was created as an object but not started yet via start() or run(). */
	NOT_STARTED,
	/** Used to indicate the timer was aborted from the outside. */
	ABORTED	
}
