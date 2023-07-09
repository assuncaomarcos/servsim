package me.marcosassuncao.servsim.util;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

import static me.marcosassuncao.servsim.SimEvent.Type.TASK_ARRIVE;

/**
 * Reads a job trace in the Standard Workload Format (SWF) and
 * creates the corresponding workload. For details on the SWF format,
 * please check:
 * <a href="http://www.cs.huji.ac.il/labs/parallel/workload/swf.html">
 *     http://www.cs.huji.ac.il/labs/parallel/workload/swf.html</a>
 *
 * @author Marcos Dias de Assuncao
 *
 */

public class SwfWorkloadReader extends EventFileReader {
    /** Default logger. */
    private static final Logger LOGGER =
            LogManager.getLogger(SwfWorkloadReader.class.getName());

    /** The list of jobs created by reading the job trace. */
    private final LinkedList<Job> jobList = new LinkedList<>();

    /** ID of the server to which the jobs will be submitted. */
    private final int dstServerId;

    /**
     * Creates a new event file reader.
     * @param eventFileName the file that contains the event information
     * @param dstServerId the id of the server entity to
     *                    which requests are sent
     * @throws IllegalArgumentException if the name is <code>null</code>
     * or has size 0.
     */
    public SwfWorkloadReader(final String eventFileName,
                             final int dstServerId)
            throws IllegalArgumentException {
        super("Workload Generator", eventFileName);
        this.dstServerId = dstServerId;
    }

    /**
     * Processes a line read from the event file.
     * @param lineNum the data line in the file
     * @param fields the list of fields read from the file
     * @return <code>true</code> if the processing was successful;
     * <code>false</code> otherwise.
     */
    @Override
    public boolean doLineProcessing(final int lineNum,
                                    final String[] fields) {
        try {
            int jobId = Integer.parseInt(fields[0]);
            long submitTime = Long.parseLong(fields[1]);
            int duration = Integer.parseInt(fields[3]);
            int nResources = Integer.parseInt(fields[4]);

            // some logs have -1 as number of resources
            nResources = nResources > 0 ? nResources : 1;

            // Ignore jobs whose duration is negative, probably
            // they have been cancelled
            if (duration > 0) {
                Job j = new Job(jobId, duration, nResources);
                j.setOwnerEntityId(super.getId());
                super.send(this.dstServerId,
                        submitTime, TASK_ARRIVE, j);
                this.jobList.add(j);
            }

        } catch (Exception ex) {
            LOGGER.error("Error parsing line of workload ", ex);
        }

        return false;
    }

    @Override
    public void doFinalProcessing() {  }

    @Override
    public void onJobReceived(final int src, final Job job) { }

    @Override
    public void onReservationComplete(final int src,
                    final Reservation res) { }

    @Override
    public void onReservationResponse(final int src,
                    final Reservation res) { }
}
