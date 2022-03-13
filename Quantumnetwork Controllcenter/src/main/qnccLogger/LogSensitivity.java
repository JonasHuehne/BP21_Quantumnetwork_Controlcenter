package qnccLogger;

public enum LogSensitivity {
	INFO,	//saves everything logged with severity INFO and higher into logfile
	WARNING,//saves everything logged of severity WARNING and higher into logfile, excludes Info
	ERROR	//saves only Errors into logfile, excludes Info and Warnings
}
