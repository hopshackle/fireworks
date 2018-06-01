package com.fossgalaxy.games.fireworks.ai.hopshackle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class EntityLog {

	public static String logDir =  "C:\\Simulation\\logs";
	public static String newline = System.getProperty("line.separator");
	protected File logFile;
	protected boolean logFileOpen;
	protected FileWriter logWriter;

	public EntityLog(String logFileName) {
		logFile = new File(logDir + File.separator + logFileName + ".txt");
		logFileOpen = false;
	}

	public void log(String message) {
		if (!logFileOpen) {
			try {
				logWriter = new FileWriter(logFile, true);
				logFileOpen = true;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		try {
			logWriter.write(message+newline);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void rename(String newName) {
		close();
		logFile.renameTo(new File(logDir + File.separator + newName + ".txt"));
	}

	public void flush() {
		if (logFileOpen) {
			try {
				logWriter.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (logFileOpen)
			try {
				logWriter.close();
				logFileOpen = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
