package me.marcosassuncao.servsim.util;

import static me.marcosassuncao.servsim.SimEvent.Type.TASK_ARRIVE;

import java.util.LinkedList;

import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads a job trace in the Standard Workload Format (SWF) and 
 * creates the corresponding workload.
 * 
 * @author Marcos Dias de Assuncao
 *
 * @see {@linkplain http://www.cs.huji.ac.il/labs/parallel/workload/swf.html}
 */

public class SwfWorkloadReader extends EventFileReader {
	private static final Logger log = LogManager.getLogger(SwfWorkloadReader.class.getName());
	private LinkedList<Job> jobList = new LinkedList<Job>();
	private int dstServerId;
	
	/**
	 * Creates a new event file reader.
	 * @param eventFileName the file that contains the event information
	 * @param dstServerId the id of the server entity to which requests are sent
	 * @throws IllegalArgumentException if the name is <code>null</code> 
	 * or has size 0.
	 */
	public SwfWorkloadReader(String eventFileName, int dstServerId)
			throws IllegalArgumentException {
		super("Workload Generator", eventFileName);
		this.dstServerId = dstServerId;
	}

	@Override
	public boolean doLineProcessing(int lineNum, String[] fields) {
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
				super.send(this.dstServerId, submitTime, TASK_ARRIVE, j);
				this.jobList.add(j);
			}
			
		} catch (Exception ex) {
			log.error("Error parsing line of workload ", ex);
		}
		
		return false;
	}

	@Override
	public void doFinalProcessing() {  }

	@Override
	public void onJobReceived(int src, Job job) {  }

	@Override
	public void onReservationComplete(int src, Reservation res) { }

	@Override
	public void onReservationResponse(int src, Reservation res) { }
}
