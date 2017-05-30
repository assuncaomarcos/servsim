package me.marcosassuncao.servsim.server;

import static com.google.common.base.Preconditions.checkArgument;
import static me.marcosassuncao.servsim.SimEvent.Type.RESERVATION_COMPLETE;
import static me.marcosassuncao.servsim.SimEvent.Type.RESERVATION_REQUEST;
import static me.marcosassuncao.servsim.SimEvent.Type.RESERVATION_RESPONSE;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link ReservationServerUser} is an entity that handles making and
 * confirming reservation requests.
 * 
 * @author Marcos Dias de Assuncao
 */

public abstract class ReservationServerUser extends ServerUser {
	private static final Logger log = LogManager.getLogger(ReservationServerUser.class.getName());

	/**
	 * Creates a new reservation user
	 * @param name the name for the user
	 * this reservation keeping entity sends requests 
	 */
	public ReservationServerUser(String name) {
		super(name);
	}
	
	/**
	 * Makes a new reservation
	 * @param srvId the id of the server whose resources are to be reserved
	 * @param delay time at which the reservation request show be made
	 * @param start the expected start time of the reservation
	 * @param duration the reservation duration
	 * @param numRes number of required resources
	 * @return the reservation
	 */
	public Reservation reserveResources(int srvId, long delay, long start, 
			long duration, int numRes) {
		checkArgument(srvId >= 0, "Invalid server Id");
		checkArgument(start >= 0, "Invalid start time");
		checkArgument(duration > 0, "Duration must be > 0");
		checkArgument(numRes > 0, "Number of resources must be > 0");
		Reservation res = new Reservation(start, duration, numRes);
		res.setOwnerEntityId(super.getId());
		super.send(srvId, delay, RESERVATION_REQUEST, res);
		return res;
	}
	
	/**
	 * Makes a new reservation
	 * @param srvId the id of the server whose resources are to be reserved
	 * @param start the expected start time of the reservation
	 * @param duration the reservation duration
	 * @param numRes number of required resources
	 * @return the reservation
	 */
	public Reservation reserveResources(int srvId, long start, long duration, int numRes) {
		return reserveResources(srvId, SimEvent.SEND_NOW, start, duration, numRes);
	}
	
	/**
	 * Makes a new immediate reservation
	 * @param srvId the id of the server whose resources are to be reserved
	 * @param duration the reservation duration
	 * @param numRes number of required resources
	 * @return the reservation
	 */
	public Reservation reserveResources(int srvId, long duration, int numRes) {
		return reserveResources(srvId, super.currentTime(), duration, numRes);
	}
	
	/**
	 * Submits a job to a given entity, such as a server
	 * @param dst the id of the destination entity
	 * @param res the reservation made for the job
	 * @param delay the delay to wait before submitting the job
	 * @param job the job to be submitted
	 */
	public void submitJob(int dst, Reservation res, long delay, Job job) {
		job.setReservationId(res.getId());
		super.submitJob(dst, delay, job);
	}
	
	/**
	 * Submits a job to a given entity, such as a server
	 * @param dst the id of the destination entity
	 * @param res the reservation made for the job
	 * @param job the job to be submitted
	 */
	public void submitJob(int dst, Reservation res, Job job) {
		submitJob(dst, res, SimEvent.SEND_NOW, job);
	}

	@Override
	public void process(SimEvent ev) {
		if (ev.type() == RESERVATION_RESPONSE) {
			try {
				Reservation res = (Reservation)ev.content();
				onReservationResponse(ev.source(), res);
			} catch (ClassCastException e) {
				log.error("Invalid reservation object.", e);
			}
		} else if (ev.type() == RESERVATION_COMPLETE) {
			try {
				Reservation res = (Reservation)ev.content();
				onReservationComplete(ev.source(), res);
			} catch (ClassCastException e) {
				log.error("Invalid reservation object.", e);
			}
		} else {
			super.process(ev);
		}		
	}
	
	/**
	 * Called when entity received a reservation(response) back from a server
	 * @param src the source of the job received
	 * @param res the reservation received
	 */
	public abstract void onReservationResponse(int src, Reservation res);
	

	/**
	 * Called when entity received a reservation back from a server
	 * @param src the source of the job received
	 * @param res the reservation received
	 */
	public abstract void onReservationComplete(int src, Reservation res);
	
}
