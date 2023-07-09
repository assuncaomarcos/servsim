package me.marcosassuncao.servsim.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class used to create a simulation report.
 *
 * @author Marcos Dias de Assuncao
 *
 * @see SimEntity
 * @see SimEvent
 */

public abstract class AbstractReport extends SimEntity {
    /**
     * Logger object.
     */
    private static final Logger LOGGER =
            LogManager.getLogger(AbstractReport.class.getName());
    /**
     * The report writer.
     */
    private PrintWriter report;

    /**
     * The path to the file where the report is stored.
     */
    private final String filePath;

    /**
     * Creates a new report.
     * @param filePath the path of the file to be created
     * @throws IllegalArgumentException if a file name is not provided.
     */
    public AbstractReport(final String filePath)
            throws IllegalArgumentException {
        super("Report_" + UUID.randomUUID());
        if (filePath == null || filePath.length() == 0) {
            throw new IllegalArgumentException("File name must be provided");
        }
        this.filePath = filePath;
    }

    /**
     * Gets the path of the file created by this simulation report.
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        try {
            report = new PrintWriter(
                    new BufferedWriter(new FileWriter(filePath)));
            onOpenLog(report);
        } catch (IOException ioe) {
            LOGGER.fatal("Error creating simulation report", ioe);
        }
    }

    @Override
    public void process(final SimEvent ev) { }

    /**
     * Adds a given line to the log.
     * @param content the line to be written to the log
     */
    public void writeToLog(final String content) {
        try {
            report.write(content);
        } catch (Exception e) {
            LOGGER.fatal("Error writing to log", e);
        }
    }

    /**
     * Called before flushing the content to the log file and closing it.
     * @param writer the writer used to write content to the file
     */
    public abstract void onFinishLog(PrintWriter writer);

    /**
     * Called after opening the log file.
     * @param writer the writer used to write content to the file
     */
    public abstract void onOpenLog(PrintWriter writer);

    /**
     * Gets the writer to which to print simulation information.
     * @return the writer to print simulation information
     */
    protected PrintWriter getWriter() {
        return this.report;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown() {
        try {
            onFinishLog(report);
            report.close();
        } catch (Exception e) {
            LOGGER.error("Error closing simulation report", e);
        }
    }
}
