package networkConnection;

/**This Enum expresses the State of a NetworkTimeoutThread
 * 
 * @author Jonas Huehne
 *
 */
public enum NetworkTimerState {
RUNNING,	//This means the timer has started, but the timeout duration has not yet elapsed.
TIMED_OUT,	//This means the timer was started and the timeout duration has elapsed. The timer will have called the method it was supplied.
NOT_STARTED,	//This means the timer was created as an object but not started yet via start() or run().
ABORTED	//This means the timer was aborted form the outside as it is not needed anymore.
}
