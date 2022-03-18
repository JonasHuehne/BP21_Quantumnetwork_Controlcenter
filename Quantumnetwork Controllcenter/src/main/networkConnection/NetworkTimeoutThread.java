package networkConnection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

import qnccLogger.Log;
import qnccLogger.LogSensitivity;

/**This Utility can be used to easily setup Timeouts.
 * It needs to be supplied the duration until the timeout is happening, 
 * a reference to the object that wants to set up the Timeout,
 * and a Method to call once the timeout happened.
 * To start the timer, create an object of it and then call start() on it.
 * The timer can be aborted with abortTimer().
 * 
 * @author Jonas Huehne
 *
 */
public class NetworkTimeoutThread extends Thread{

	int msDuration;	//The duration until TimeOut
	Instant startWait;	//the time when the timer started
	Instant current;	//the current time
	NetworkTimerState state = NetworkTimerState.NOT_STARTED;	//the timer state
	String timeoutMessage = "TIMEOUT!";	//the timeout message
	Method methodToCall;	//the message to call after timeout
	Object caller;	//the object that requested the timer
	Object[] args;	//dummy arguments for the method call on timeout, not supported
	private static Log log = new Log(NetworkTimeoutThread.class.getName(), LogSensitivity.WARNING);
	
	/**
	 * 
	 * This Utility can be used to easily setup Timeouts.
     * It needs to be supplied the duration until the timeout is happening,
     * a reference to the object that wants to set up the Timeout,
     * and a Method to call once the timeout happened.
	 * 
     * To start the timer, create an object of it and then call start() on it.
     * The timer can be aborted with abortTimer().
	 * 
	 * @param ms the duration until the timeout happens in ms.
	 * @param caller the object that wants to be notified of the timeout.
	 * @param method the method of the caller object that should get called after the timeout.
	 */
	public NetworkTimeoutThread(int ms, Object caller, Method method){
		msDuration = ms;
		startWait = Instant.now();
		current = Instant.now();
		methodToCall = method;
		this.caller = caller;
		this.args = new Object[0];
	}
	
	/**
	 * 
	 * This Utility can be used to easily setup Timeouts.
     * It needs to be supplied the duration until the timeout is happening,
     * a reference to the object that wants to set up the Timeout,
     * and a Method to call once the timeout happened.
     * 
     * To start the timer, create an object of it and then call start() on it.
     * The timer can be aborted with abortTimer().
	 * 
	 * @param ms the duration until the timeout happens in ms.
	 * @param caller the object that wants to be notified of the timeout.
	 * @param method the method of the caller object that should get called after the timeout.
	 * @param timeoutMessage this overwrites the String that will be printed on timeout. Can be used for debugging. Can be "" if no log messages is needed.
	 */
	NetworkTimeoutThread(int ms, Object caller, Method method, String timeoutMessage){
		msDuration = ms;
		startWait = Instant.now();
		current = Instant.now();
		methodToCall = method;
		this.caller = caller;
		this.args = new Object[0];
	}
	
	/**
	 * 
	 * This Utility can be used to easily setup Timeouts.
     * It needs to be supplied the duration until the timeout is happening
     * and reference to the object that wants to set up the Timeout.
     * 
     * To start the timer, create an object of it and then call start() on it.
     * The timer can be aborted with abortTimer().
	 * 
	 * @param ms the duration until the timeout happens in ms.
	 * @param timeoutMessage this overwrites the String that will be printed on timeout. Can be used for debugging.
	 */
	NetworkTimeoutThread(int ms, String timeoutMessage){
		msDuration = ms;
		startWait = Instant.now();
		current = Instant.now();
		methodToCall = null;
		this.args = new Object[0];
	}
	
	/**
	 * This method is called when the timeout happens and calls the method that was supplied to the constructor if applicable.
	 */
	private void onTimeout() {
		if(methodToCall == null) {
			return;
		}
		try {
			methodToCall.invoke(caller, args);
		} catch (Exception e) {
			log.logError("Exception in NetworkTimer!", e);
		} 
	}
	
	/**
	 * This method can be used to abort the timer once the timeout is no longer required,
	 * likely because the task in question has been completed.
	 */
	public void abortTimer() {
		state = NetworkTimerState.ABORTED;
		this.interrupt();
	}
	
	/**This returns the time the Timer has to go until Timeout in ms.
	 * It returns -1 if the Timer was aborted, the total duration if it was not started yet
	 * and 0 if the timeout happened already.
	 * Also returns -1 as a fallback in case the NetworkTimerState was an unexpected State.
	 * 
	 * @return the time until Timeout in ms.
	 */
	public int getRemainingTime() {
		switch(state) {
		case ABORTED: return -1;

		case NOT_STARTED: return msDuration;

		case RUNNING: return (msDuration - (int) Duration.between(startWait, current).toMillis());
			
		case TIMED_OUT: return 0;

		default: return -1;
		}
	}
	
	
	public void run() {
		if(state == NetworkTimerState.NOT_STARTED) {
			state = NetworkTimerState.RUNNING;
			while(state == NetworkTimerState.RUNNING) {
				current = Instant.now();
				if(Duration.between(startWait, current).toMillis() >= msDuration) {
					state = NetworkTimerState.TIMED_OUT;
					if(!timeoutMessage.equals("")) {
						log.logWarning(timeoutMessage);
					}
					onTimeout();
					
				}
			}
		}		
	}
}
