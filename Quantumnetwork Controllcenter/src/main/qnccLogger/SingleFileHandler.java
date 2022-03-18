package qnccLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

import frame.Configuration;

/**
 * singlet of {@link java.util.logging.FileHandler}
 * used to let all loggers of project write to the same log file
 * 
 * @author Lukas Dentler
 *
 */
public class SingleFileHandler {
	private static SingleFileHandler singleFileHandler;
	private FileHandler fileHandler;
	private static final String FILE_NAME = "log.txt";
	private static final Path currentWorkingDir = Paths.get("").toAbsolutePath();
	private static final Path propertiesDir = currentWorkingDir.resolve("properties");
	
	/**
	 * creates the SingleFileHandler
	 * 
	 * since this is a singlet of {@link java.util.logging.FileHandler}
	 * if there was already one created
	 * the existing instance of SingleFileHandler is referenced 
	 */
	public SingleFileHandler() {
		//make SingleFileHandler a singleton
		if (singleFileHandler != null) {
			return;
		}
		
		//get current Date and Time
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu_MM_dd_HH_mm_");
        LocalDateTime now = LocalDateTime.now();
        String currentDateTime = dateTimeFormatter.format(now);
        
        // get logs directory;
        Path qnccPath = Paths.get(Configuration.getBaseDirPath());
    	Path logsPath = qnccPath.resolve("logs");
    	
    	//create logs directory if it does not exist
    	if(!logsPath.toFile().isDirectory()) {
    		Configuration.createFolders();
    	}

    	//create log file
    	String fileName = currentDateTime + FILE_NAME;
		File file = logsPath.resolve(fileName).toFile();
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.err.println(e.toString());
			}
		}
		
		//Create Properties Folder
		if(!propertiesDir.toFile().isDirectory()) {
			try {
				Files.createDirectory(propertiesDir);
			} catch (IOException e) {
				System.err.println("Failed to create Log Property Folder! " + e);
			}
    	}
		
		//Create Property File
		Path myLoggingPropertiesPath = propertiesDir.resolve("logger.properties");
		File myLoggingProperties = myLoggingPropertiesPath.toFile();
		if(!myLoggingProperties.isFile()) {
			try {
				Files.createFile(myLoggingPropertiesPath); 
				String properties = "java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.SimpleFormatter.format=[%1$tF %1$tT]   [%4$s]: %n	[at]:				%3$s %n	[Dev Message]:		%5$s %n	[Stacktrace]:%n%6$s%n------------------------------------------------------------%n";
				Files.writeString(myLoggingPropertiesPath, properties);
			} catch (IOException e) {
				System.err.println(e.toString());
			}
		}

		//set logging properties
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(myLoggingProperties));
		} catch (SecurityException | IOException e) {
			System.err.println(e.toString());
		}
		
		//create FileHandler
		try {
			fileHandler = new FileHandler(file.getAbsolutePath(), true);
		} catch (SecurityException | IOException e) {
			System.err.println(e.toString());
		}
		
		singleFileHandler = this;
	}
	
	/**
	 * 
	 * @return the singlet FileHandler instance
	 */
	public FileHandler getFileHandler() {
		return singleFileHandler.fileHandler;
	}
	
}
