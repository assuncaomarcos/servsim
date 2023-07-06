package me.marcosassuncao.servsim.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import me.marcosassuncao.servsim.server.ReservationServerUser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class implements the basic functionality for an entity that loads event 
 * information from a file. This reader loads a file, and calls hooks that 
 * define the basic behaviour for the parsed information 
 * (e.g. {@link #doLineProcessing(int, String[])}).
 * 
 * @author Marcos Dias de Assuncao
 */

public abstract class EventFileReader extends ReservationServerUser { 
	private static final Logger log = LogManager.getLogger(EventFileReader.class.getName());
	private static final String COMMENT = "#";  	// Comment symbol 
	private final String fileName;						// the event file name
	private final String delimiter;

	/**
	 * Creates a new event file reader.
	 * @param name the name of the entity
	 * @param eventFileName the file that contains the event information
	 * @param delim the field delimiter, default is "\\s+"
	 * @throws IllegalArgumentException if the name is <code>null</code> 
	 * or has size 0.
	 */
	public EventFileReader(String name, 
			String eventFileName, String delim) throws IllegalArgumentException {
		super(name);
		this.fileName = eventFileName;
		this.delimiter = delim;
	}
	
	/**
	 * Creates a new event file reader.
	 * @param name the name of the entity
	 * @param eventFileName the file that contains the event information
	 * @throws IllegalArgumentException if the name is <code>null</code> 
	 * or has size 0.
	 */
	public EventFileReader(String name, 
			String eventFileName) throws IllegalArgumentException {
		this(name, eventFileName, "\\s+");
	}
	
	@Override
	public void onStart() {
		BufferedReader reader;
        String line;
        String[] sp;
        int lineNum = 0;
               
		try {
			log.info("Loading events from file: " + fileName);
			FileInputStream file = new FileInputStream(fileName);
			InputStreamReader input = new InputStreamReader(file);
			reader = new BufferedReader(input);

			while (reader.ready()) {
				line = reader.readLine();
				line = line.trim();
				lineNum++;
				
				if (line.startsWith(COMMENT) || line.length() == 0) {
					continue;
				}
				
				sp = line.split(this.delimiter);
				doLineProcessing(lineNum, sp);			
			}
			
			reader.close();
			doFinalProcessing();
		} catch (FileNotFoundException fe) {
			log.fatal("File not found ", fe);
		} catch (IOException ioe) {
			log.fatal("Error reading file ", ioe);
		}
	}
	
	/**
	 * Processes a line read from the event file.
	 * @param lineNum the data line in the file
	 * @param fields the list of fields read from the file
	 * @return <code>true</code> if the processing was successful;
	 * <code>false</code> otherwise.
	 */
	public abstract boolean doLineProcessing(int lineNum, String[] fields);
	
	/**
	 * Method invoked after the whole event file has been read. It can
	 * be used by subclasses to do some final processing.
	 */
	public abstract void doFinalProcessing();
	
}
