package qnccLogger;

/**
 * Enum for different log sensitivities
 * @author Lukas Dentler
 */
public enum LogSensitivity {
	INFO,	//saves everything logged with severity INFO and higher into logfile
	WARNING,//saves everything logged of severity WARNING and higher into logfile, excludes Info
	ERROR	//saves only Errors into logfile, excludes Info and Warnings
}
