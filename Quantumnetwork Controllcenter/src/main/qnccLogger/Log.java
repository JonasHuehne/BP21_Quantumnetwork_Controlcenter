package qnccLogger;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The logger for errorhandling in this project
 * 
 * @author lukas
 *
 */
public class Log {
	private Logger logger;
	private FileHandler fileHandler = new SingleFileHandler().getFileHandler();
	
	/**
	 * crates the logger with a given Name <br>
	 * {@link loggerName} appears after "[at]: " in log file <br>
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
	 * log an Error in logfile with a devmessage and name, message and stacktrace of corresponding Exception
	 * 
	 * @param message devmessage to be shown in logfile
	 * @param exception the corresponding Exception
	 */
	public void logError(String message, Throwable exception) {
		logger.log(Level.SEVERE, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log a Warning in logfile with a devmessage and name, message and stacktrace of corresponding Exception
	 * @param message devmessage to be shown in logfile
	 * @param exception the corresponding Exception
	 */
	public void logWarning(String message, Exception exception) {
		logger.log(Level.WARNING, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log a Warning in logfile with a devmessage and without a corresponding Exception
	 * @param message devmessage to be shown in logfile
	 */
	public void logWarning(String message) {
		logger.log(Level.WARNING, message);
	}
	
	/**
	 * log a devmessage as DebugInfo in logfile
	 * use a {@link GetCodeLine} Exception to show the code line in which the log was called in stacktrace
	 * @param message a devmessage to be shown in logfile
	 * @param exception the corresponding exception or {@link GetCodeLine} in order to see the code line where the log was called
	 */
	public void logDebugInfo(String message, Throwable exception) {
		logger.log(Level.INFO, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	/**
	 * log a devmessage as Info in logfile
	 * @param message a devmessage to be shown in logfile
	 */
	public void logInfo(String message) {
		logger.log(Level.INFO, message);
	}
	
	/**
	 * used to set sensitivity of logger to INFO<br>
	 * <br>
	 * logs everything logged of severity INFO and higher
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
