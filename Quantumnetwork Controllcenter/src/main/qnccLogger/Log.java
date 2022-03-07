package qnccLogger;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The logger for errorhandling in this project
 * 
 * @author Lukas Dentler
 *
 */
public class Log {
	private Logger logger;
	private FileHandler fileHandler = new SingleFileHandler().getFileHandler();
	
	/**
	 * creates the logger with a given Name <br>
	 * parameter loggerName appears after "[at]: " in log file <br>
	 * for best orientation in log file use:
	 *  *classname*.class.getName() as loggerName<br>
	 *  example: QuantumnetworkControllcenter.class.getName()
	 * 
	 * @param loggerName the Name of the logger
	 */
	public Log(String loggerName) {
		logger = Logger.getLogger(loggerName);
		logger.addHandler(fileHandler); 
		logger.setLevel(Level.WARNING);
	}
	
	/**
	 * log an Error in logfile with a message shown after "[Dev Message]:" in logfile
	 *  and name, message and stacktrace of corresponding Exception
	 * 
	 * @param message message to be shown after "[Dev Message]:" in logfile
	 * @param exception the corresponding Exception
	 */
	public void logError(String message, Throwable exception) {
		logger.log(Level.SEVERE, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log a Warning in logfile with a message shown after "[Dev Message]:" in logfile
	 *  and name, message and stacktrace of corresponding Exception
	 * 
	 * @param message message to be shown after "[Dev Message]:" in logfile
	 * @param exception the corresponding Exception
	 */
	public void logWarning(String message, Exception exception) {
		logger.log(Level.WARNING, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log a Warning in logfile with a message shown after "[Dev Message]:" in logfile
	 * and without a corresponding Exception
	 * @param message message to be shown after "[Dev Message]:" in logfile
	 */
	public void logWarning(String message) {
		logger.log(Level.WARNING, message);
	}
	
	/**
	 * log a DebugInfo in logfile with a message shown after "[Dev Message]:" in logfile
	 * and name, message and stacktrace of corresponding Exception
	 * 
	 * @param message message to be shown after "[Dev Message]:" in logfile
	 * @param exception the corresponding exception 
	 */
	public void logDebugInfo(String message, Throwable exception) {
		logger.log(Level.INFO, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log an Info in logfile with a message shown after "[Dev Message]:" in logfile
	 * and without a corresponding Exception
	 * @param message message to be shown after "[Dev Message]:" in logfile
	 */
	public void logInfo(String message) {
		logger.log(Level.INFO, message);
	}
	
	/**
	 * used to set sensitivity of logger to INFO<br>
	 * <br>
	 * saves everything logged with severity INFO and higher into logfile
	 */
	public void loggerShowInfos() {
		logger.setLevel(Level.INFO);
	}
	
	/**
	 * used to set sensitivity of logger to WARNING<br>
	 * <br>
	 * logs everything logged of severity WARNING and higher <br>
	 * excludes Info and DebugInfo
	 */
	public void loggerShowWarnings() {
		logger.setLevel(Level.WARNING);
	}
	
	/**
	 * used to set sensitivity of logger to SEVERE<br>
	 * <br>
	 * logs only Errors
	 * excludes Info, DebugInfo and Warnings
	 */
	public void loggerShowErrors() {
		logger.setLevel(Level.SEVERE);
	}
}
