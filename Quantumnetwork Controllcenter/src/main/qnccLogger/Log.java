package qnccLogger;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Log {
	private Logger logger;
	private FileHandler fileHandler = new SingleFileHandler().getFileHandler();
	
	public Log(String loggerName) {
		logger = Logger.getLogger(loggerName);
		logger.addHandler(fileHandler); 
		logger.setLevel(Level.WARNING);
	}
	
	public void logError(String message, Throwable exception) {
		logger.log(Level.SEVERE, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	public void logWarning(String message, Exception exception) {
		logger.log(Level.WARNING, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	public void logWarning(String message) {
		logger.log(Level.WARNING, message);
	}
	
	public void logDebugInfo(String message, Throwable exception) {
		logger.log(Level.INFO, message + "\n \t[Exception Name]:\t" + exception.getClass().getSimpleName() + "\n\t[Exception Message]:" + exception.getMessage(), exception);
	}
	
	public void logInfo(String message) {
		logger.log(Level.INFO, message);
	}
	
	public void loggerShowInfos() {
		logger.setLevel(Level.INFO);
	}
	
	public void loggerShowWarnings() {
		logger.setLevel(Level.WARNING);
	}
	
	public void loggerShowErrors() {
		logger.setLevel(Level.SEVERE);
	}
}
