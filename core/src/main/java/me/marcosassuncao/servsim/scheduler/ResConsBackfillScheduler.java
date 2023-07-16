package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;
import me.marcosassuncao.servsim.job.WorkUnit;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.profile.SingleProfile;
import me.marcosassuncao.servsim.server.ResourcePool;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.ServerAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

import static me.marcosassuncao.servsim.SimEvent.Type.*;
import static me.marcosassuncao.servsim.job.WorkUnit.Status.*;

/**
 * {@link ResConsBackfillScheduler} class is an allocation policy for
 * {@link Server} that implements conservative backfilling and
 * supports advance reservations. The policy is based on the conservative
 * backfilling algorithm described in the following papers:
 * <ul>
 * 		<li> Dror G. Feitelson and Ahuva Mu'alem Weil, Utilization and
 * 		Predictability in Scheduling the IBM SP2 with Backfilling, in
 * 		Proceedings of the 12th International Parallel Processing Symposium on
 * 		International Parallel Processing Symposium (IPPS 1998), pp. 542-546.
 *
 * 		<li>Ahuva W. Mu'alem and Dror G. Feitelson, Utilization, Predictability,
 * 		Workloads, and User Runtime Estimates in Scheduling the IBM SP2
 * 		with Backfilling. IEEE Transactions on Parallel and Distributed
 * 		Systems, 12:(6), pp. 529-543, 2001.
 * </ul>
 * Similarly to {@link ConsBackfillScheduler} this scheduler relies on the
 * availability profile provided by {@link ResourcePool}. The difference is
 * that in many cases an advance reservation will require two entries
 * in the profile, namely to mark its start time and its finish time.
 * In addition, when a job is cancelled, advance reservations are not
 * removed from the availability profile and therefore are not moved
 * forwards in the scheduling queue.
 * <br>
 * This scheduler supports parallel jobs and some AR functionalities,
 * such as:
 * <ul>
 *    <li> process a new reservation
 *    <li> cancel a reservation
 * </ul>
 * @author  Marcos Dias de Assuncao
 */

public class ResConsBackfillScheduler extends ConsBackfillScheduler implements ReservationScheduler {
	private static final Logger log = LogManager.getLogger(ResConsBackfillScheduler.class.getName());
	/**
	 * The resource reservations.
	 */
	protected LinkedHashMap<Integer, Reservation> reservations = new LinkedHashMap<>();

	/**
	 * Maintain an availability profile where the time slots
	 * allocated to reservations are inserted into.
	 */
	protected SingleProfile profileRes;

	/**
	 * Creates a new scheduler instance
	 */
	public ResConsBackfillScheduler() {
		super(ResConsBackfillScheduler.class.getSimpleName() + "-" + UUID.randomUUID());
	}

	/**
	 * Creates a new scheduler instance.
	 * @param name a name for the simulation entity
	 * @throws IllegalArgumentException the name is <code>null</code>
	 */
	public ResConsBackfillScheduler(String name) throws IllegalArgumentException {
		super(name);
	}

	@Override
	public void initialize(ServerAttributes attr) {
		super.initialize(attr);

		// Create the profile, and add an entry to make resources
		// unavailable to reservations as simulations have not started yet
		int capacity = attr.getResourcePool().getCapacity();
		this.profileRes = new SingleProfile(capacity);
		RangeList rlist = new RangeList(0, capacity - 1);
		this.profileRes.allocateResourceRanges(rlist, 0, Long.MAX_VALUE);
	}

	@Override
	public void doReservationProcessing(Reservation r) {
		ResourcePool resources = super.serverAttributes().getResourcePool();
		long startTime = r.getRequestedStartTime();
		ProfileEntry e = resources.checkAvailability(r.getNumReqResources(), startTime, r.getDuration());

		if (e != null) {
			RangeList selected = e.getAvailRanges().selectResources(r.getNumReqResources());
			allocateResourcesToReservation(r, selected);
			reservations.put(r.getId(), r);
			log.trace("Starting reservation #" + r.getId() + " at " + super.currentTime());

			// add time slot to the reservations' profile
			this.profileRes.addTimeSlot(r.getRequestedStartTime(),
					r.getRequestedStartTime() + r.getDuration(), selected);
		} else {
			setReservationStatus(r, FAILED);
		}

		// Inform reservation keeper about reservation status
		super.send(r.getOwnerEntityId(), 0, RESERVATION_RESPONSE, r);
	}

	@Override
	public void doReservationCompletion(Reservation r) {
		log.trace("Completing reservation #" + r.getId() +
						  " at " + super.currentTime());

		setReservationStatus(r, COMPLETE);
		this.reservations.remove(r.getId());

		if (log.isTraceEnabled()) {
			log.trace("Completed reservation: \n" + r.getId());
		}
		sendReservationToOwner(r);
	}

	@Override
	public void doReservationCancel(int id) {
		if (this.reservations.containsKey(id)) {
			Reservation r = this.reservations.get(id);
			long startTime = r.getRequestedStartTime();
			Collection<Integer> affectedJobIds = super.compressSchedule(startTime);

			// Cancel all jobs using the resources allocated by the reservation
			Iterator<Job> it = this.runningQueue.iterator();
			while (it.hasNext()) {
				Job j = it.next();
				if (j.hasReserved() && j.getReservationId() == id) {
					affectedJobIds.add(j.getId());
					super.setJobStatus(j, CANCELLED);
					sendJobToOwner(j);
					it.remove();
				}
			}

			it = this.waitingQueue.iterator();
			while (it.hasNext()) {
				Job j = it.next();
				if (j.hasReserved() && j.getReservationId() == id) {
					affectedJobIds.add(j.getId());
					super.setJobStatus(j, CANCELLED);
					sendJobToOwner(j);
					it.remove();
				}
			}

			super.cancelJobEvents(affectedJobIds);

			// return time slot allocated for the reservation
			ResourcePool rlist = super.serverAttributes().getResourcePool();
			rlist.releaseResources(startTime, startTime + r.getDuration(), r.getResourceRanges());
			this.profileRes.allocateResourceRanges(r.getResourceRanges(), startTime, startTime+r.getDuration());

			// Finally, reschedule jobs
			super.rescheduleJobs();
		}
	}

	@Override
	public void doJobProcessing(Job job) {
		if (job.hasReserved()) {
			// Select from what has been reserved
			Reservation r = this.reservations.get(job.getReservationId());
			long startTime = Math.max(r.getRequestedStartTime(), super.currentTime());
			ProfileEntry e = this.profileRes.checkAvailability(job.getNumReqResources(), startTime, job.getDuration());
			boolean success = true;

			if (e != null && e.getAvailRanges().getNumItems() >= job.getNumReqResources()) {
				// Finds the intersection between what's available for reservations and
				// what the job's reservation allocated. The intersection represents
				// the resources that are still left to be used
				RangeList availRanges = e.getAvailRanges().intersection(r.getResourceRanges());
				RangeList selected = availRanges.selectResources(job.getNumReqResources());

				if (selected.getNumItems() >= job.getNumReqResources()) {
					scheduleReservationJob(startTime, job, selected);
				} else {
					success = false;
				}
			} else {
				success = false;
			}

			if (!success) {
				super.setJobStatus(job, FAILED);
				sendJobToOwner(job);
				log.error("Insuficient resources reserved for job: " + job +
						" with reservation: " + r);
			}
		} else {
			// normal backfilling scheduling
			super.doJobProcessing(job);
		}
	}

	/**
	 * Allocates a resource to a given job for which a reservation
	 * has previously been made. Schedules a {@link SimEvent.Type#TASK_COMPLETE}
	 * event to be handled at job completion and a {@link SimEvent.Type#TASK_START}
	 * if the job's start time is in the future
	 * @param time the start time of the allocation
	 * @param job the job to which the resource will be allocated
	 * @param res the resource range list to be allocated.
	 */
	protected void scheduleReservationJob(long time, Job job, RangeList res) {
		long now = super.currentTime();
		this.profileRes.allocateResourceRanges(res, time, time + job.getDuration());

		// if start time is in the future, then schedule an event to the
		// scheduler itself to signal when the task must be started
		if (time > now) {
			super.send(super.getId(), time - now, SimEvent.Type.TASK_START, job);
			setJobStatus(job, WAITING);
		} else {
			setJobStatus(job, IN_EXECUTION);
		}

		// schedule an event to be handled at the completion of the job
		super.send(super.getId(), time - now + job.getDuration(), SimEvent.Type.TASK_COMPLETE, job);

		job.setResourceRanges(res);
	}

	@Override
	public void process(SimEvent ev) {
		if (ev.type() == RESERVATION_START) {
			try {
				Reservation r = (Reservation)ev.content();
				setReservationStatus(r, IN_EXECUTION);
			} catch (ClassCastException e) {
				log.error("Invalid reservation object", e);
			}
		} else if (ev.type() == RESERVATION_COMPLETE) {
			try {
				doReservationCompletion((Reservation)ev.content());
			} catch (ClassCastException e) {
				log.error("Invalid reservation object", e);
			}
		} else {
			super.process(ev);
		}
	}

	/**
	 * Sends a reservation back to its owner. That is, schedules an event for the owner
	 * to receive the completed reservation and process it.
	 * @param r the reservation to be returned to the owner
	 */
	protected void sendReservationToOwner(Reservation r) {
		if (r.getOwnerEntityId() == -1) {
			log.trace("Reservation #" + r.getId() + " does not have an owner.");
		} else {
			super.send(r.getOwnerEntityId(), 0L, RESERVATION_COMPLETE, r);
		}
	}

	/**
	 * Allocates a resource to a given reservation and schedules a
	 * {@link SimEvent.Type#RESERVATION_COMPLETE} event to be handled at reservation
	 * completion
	 * @param r the reservation to which the resources will be allocated
	 * @param res the resource range list to be allocated.
	 */
	protected void allocateResourcesToReservation(Reservation r, RangeList res) {
		allocateResourcesToReservation(r.getRequestedStartTime(), r, res);
	}

	/**
	 * Allocates a resource to a given reservation and schedules a
	 * {@link SimEvent.Type#RESERVATION_COMPLETE} event to be handled at reservation
	 * completion
	 * @param time the start time of the allocation
	 * @param r the reservation to which the resources will be allocated
	 * @param res the resource range list to be allocated.
	 */
	protected void allocateResourcesToReservation(long time, Reservation r, RangeList res) {
		ResourcePool resources = super.serverAttributes().getResourcePool();
		long now = super.currentTime();
		resources.allocateResources(r, res, time);

		super.send(super.getId(), time - now, RESERVATION_START, r);
		setReservationStatus(r, WAITING);

		// schedule an event to be handled at the completion of the job
		super.send(super.getId(), time - now + r.getDuration(), RESERVATION_COMPLETE, r);

		r.setResourceRanges(res);
	}

	/**
	 * Helper method to set a reservation status
	 * @param r the reservation whose status is to be set
	 * @param status the new reservation status
	 */
	protected void setReservationStatus(Reservation r, WorkUnit.Status status) {
		WorkUnit.Status prevStatus = r.getStatus();
		r.setStatus(status, super.currentTime());
		super.fireStatusChange(r, prevStatus, status);
	}
}
