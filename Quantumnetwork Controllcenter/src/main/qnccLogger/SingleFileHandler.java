package qnccLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

public class SingleFileHandler {
	private static SingleFileHandler singleFileHandler;
	private FileHandler fileHandler;
	private static final String FILE_NAME = "log.txt";
	
	public SingleFileHandler() {
		//make singleFileHanler a singleton
		if (singleFileHandler != null) {
			return;
		}
		
		//create log File if it does not exist
		File file = new File(FILE_NAME);
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.err.println(e.toString());
			}
		}
		
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
		File myLoggingProperties = currentWorkingDir.resolve("mylogging.properties").toFile();
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream(myLoggingProperties));
		} catch (SecurityException | IOException e) {
			System.err.println(e.toString());
		}
		
		//create FileHandler
		try {
			fileHandler = new FileHandler(FILE_NAME, false);
		} catch (SecurityException | IOException e) {
			System.err.println(e.toString());
		}
		
		
		singleFileHandler = this;
	}
	
	public FileHandler getFileHandler() {
		return singleFileHandler.fileHandler;
	}
	
}
