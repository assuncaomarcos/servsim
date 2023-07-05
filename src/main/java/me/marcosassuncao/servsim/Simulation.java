package me.marcosassuncao.servsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class responsible for managing a simulation. A simulation has basically two event queues, 
 * one for future events and another for deferred events. The entities create events and 
 * add them to the future queue. The simulation constantly checks the future event queue, 
 * removes events that can be handled at a given clock tick and adds them to the deferred 
 * event queue. At each clock tick, after events are added to the deferred queue, 
 * the entities responsible for handling them are invoked. This is performed by calling 
 * {@link SimEntity#process(SimEvent)} of each entity object that must handle an event 
 * in the deferred queue.
 * <p>
 * To run a simulation, you need to invoke {@link #run()}
 *  
 * @author Marcos Dias de Assuncao
 */
public abstract class Simulation implements Runnable {
	private static final Logger log = LogManager.getLogger(Simulation.class.getName());
	private TreeSet<SimEvent> future = new TreeSet<>(); // future event queue
	private LinkedList<SimEvent> deferred = new LinkedList<>(); // events handled at a clock tick
	private EntityList entities = new EntityList();
	private Comparator<SimEvent> eventPriority = null;
	private SimClock clock = new SimClock(); 
	private Status status = Status.NOT_STARTED;
	
	// This is to allow a simulation to have a fixed time length.
	// Some entities in certain cases may need to schedule internal 
	// events at regular intervals. If a time span is not defined,
	// the simulation will run indefinitely as the future event
	// queue will never be empty. simSpan allows you to specify
	// a maximum time length for a simulation. And abrupt interruption
	// determines whether the simulation will stop abruptly once
	// the given time is reached
	private long timeSpan = Long.MAX_VALUE;
	private boolean abruptInterrupt = false;
	private long endWarmUp = 0L;

	/**
	 * Default constructor.
	 */
	public Simulation() {  }

	/**
	 * Method to be used to configure 
	 * the simulation before running it
	 */
	public abstract void onConfigure();
	
	/**
	 * Resets a simulation instance to be reused for testing or
	 * for running a new simulation
	 * @throws IllegalStateException if simulation is running
	 */
	public void reset() throws IllegalStateException {
		checkState(status.nextState().contains(Status.NOT_STARTED),
				"Cannot reset a simulation under state %s", status);
		
		future = new TreeSet<>(); 
		deferred = new LinkedList<>(); 
		entities = new EntityList();
		clock.reset();
		timeSpan = Long.MAX_VALUE;
		abruptInterrupt = false;
		setStatus(Status.NOT_STARTED);
	}
	
	/**
	 * This is to allow a simulation to have a fixed time length.
	 * Some entities in certain cases may need to schedule internal
	 * events at regular intervals. If a time span is not defined,
	 * the simulation will run indefinitely as the future event
	 * queue will never be empty. simSpan allows you to specify
	 * a maximum time length for a simulation. And abrupt interruption
	 * determines whether the simulation will stop abruptly once
	 * the given time is reached
	 * @param span the duration of the simulation in time units
	 * @param abruptInterrupt <code>true</code> if the simulation
	 * should stop abruptly when the time span is lapsed
	 * @throws IllegalArgumentException if the span is &lt;= 0
	 * @throws IllegalStateException if the span has already been set
	 */
	public void setTimeSpan(long span, boolean abruptInterrupt) 
			throws IllegalArgumentException, IllegalStateException {
		
		checkArgument(span > 0, "Span needs to be greater than 0");
		checkArgument(timeSpan == Long.MAX_VALUE, "Span already set: %s", timeSpan);
		
		this.timeSpan = span;
		this.abruptInterrupt = abruptInterrupt;
	}
	
	/**
	 * Sets the end of warm up phase for simulations that need it.
	 * @throws IllegalStateException if end of warm up 
	 * has already been set.
	 */
	public void setEndOfWarmUp() {
		checkState(endWarmUp == 0L, "Warm up already set: %s", endWarmUp);
		this.endWarmUp = clock.getTime();
	}
	
	/**
	 * Returns the end of warm up phase for simulations that need it.
	 * @return end of warm up
	 */
	public long getEndOfWarmUp() {
		return this.endWarmUp;
	}

	/**
	 * Returns the duration of the simulation. 
	 * @return the simulation time span
	 */
	public long getTimeSpan() {
		return this.timeSpan;
	}
		
	/**
	 * Checks whether the simulation is running.
	 * @return <code>true</code> if the simulation is running; <code>false</code> otherwise.
	 */
	public boolean isRunning() {
		return status == Status.RUNNING;
	}
	
	/**
	 * Checks if the simulation is paused
	 * @return <code>true</code> if it is paused
	 */
	public boolean isPaused() {
		return status == Status.PAUSED;
	}
	
	/**
	 * Checks if the simulation has started
	 * @return <code>true</code> if it has started
	 */
	public boolean hasStarted() {
		return !(status == Status.NOT_STARTED);
	}
	
	/**
	 * Returns the simulation status
	 * @return the status
	 * @see Status
	 */
	public Status getStatus() {
		return this.status;
	}
	
	/* Helper method to set status */
	private void setStatus(Status status) {
		synchronized(this) {
			this.status = status;
		}	
	}
	
	/**
	 * Adds an entity to the simulation;
	 * @param entity the entity to be added.
	 * @return <code>true</code> if the entity has been added; 
	 * <code>false</code> otherwise.
	 */
	public boolean registerEntity(SimEntity entity) {
		checkState(entity.getSimulation() == null, "Entity already registered with a simulation");
		entity.setSimulation(this);
		boolean success = entities.addEntity(entity);
		if (success && isRunning()) {
			entity.onStart();
		}
		return success;
	}
		
	/**
	 * Sets the clock unit to be used by the simulator.<br>
	 * <b>NOTE:</b> the default unit is {@link TimeUnit#SECONDS}.
	 * @param unit the clock unit.
	 * @see TimeUnit
	 * @throws IllegalArgumentException if the unit is invalid.
	 */
	public void setClockUnit(TimeUnit unit) {
		clock.setUnit(unit);
	}
	
	/**
	 * In some simulations, it might be important to specify a date that 
	 * represents the start time of the simulation. This information can be
	 * used to control, for example, the load or availability of resources
	 * during days of the week.
	 * @param date the start date of the simulation
	 * @throws IllegalArgumentException if <code>date</code> is <code>null</code>
	 */
	public void setStartDate(Date date) {
		checkState(!isRunning(), "Error setting start date, simulation is running");
		clock.setStartDate(checkNotNull(date));
	}
	
	/**
	 * Sets a comparator to sort events handled at each simulation clock tick.
	 * @param comp the event comparator
	 */
	public void setClockTickEventComparator(Comparator<SimEvent> comp) {
		this.eventPriority = comp;
	}
	
	/**
	 * Gets the clock used by the simulation
	 * @return the clock used by the simulation
	 */
	public SimClock getClock() {
		return this.clock;
	}
	
	/**
	 * Returns an entity with a given id
	 * @param id the entity id
	 * @return the entity or <code>null</code> if not found.
	 */
	public SimEntity getEntity(int id) {
		return entities.getEntity(id);
	}
	
	/**
	 * Returns an entity with a given name
	 * @param name the entity name
	 * @return the entity or <code>null</code> if not found.
	 */
	public SimEntity getEntity(String name) {
		return entities.getEntity(name);
	}
	
	/**
	 * Gets a list of entities that match the filtering criteria.
	 * @param filter the filter used to select the entities.
	 * @return the entities that match the filtering criteria.
	 */
	public List<SimEntity> getEntities(Predicate<SimEntity> filter) {
		return entities.getEntities(filter);
	}
	
	/**
	 * Returns the number of events in the future queue that match 
	 * a given filtering criterion
	 * @param filter the filter to be used
	 * @return the number of events
	 */
	public int countFutureEvents(Predicate<SimEvent> filter) {
		int count = 0;
		Iterator<SimEvent> it = future.iterator();
		while (it.hasNext()) {
			SimEvent ev = it.next();
			if(filter.test(ev)) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Checks if the future queue has at least one event that matches 
	 * a given filtering criterion
	 * @param filter the filter to be used
	 * @return <code>true</code> if an event that matches the filter has
	 * 			been found; <code>false</code> otherwise.
	 */
	public boolean hasFutureEvent(Predicate<SimEvent> filter) {
		Iterator<SimEvent> it = future.iterator();
		while (it.hasNext()) {
			SimEvent ev = it.next();
			if(filter.test(ev)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Cancels future events that match a given filtering criterion
	 * @param filter the filter to be used
	 */
	public void cancelFutureEvents(Predicate<SimEvent> filter) {
		future.removeIf(filter);
	}
	
	/**
	 * Cancels the next future event that matches a given 
	 * filtering criterion
	 * @param filter the filter to be used
	 * <br>
	 * <b>NOTE:</b> if you want to cancel all events that match 
	 * the provided criterion, use {@link #cancelFutureEvents(Predicate)}
	 */
	public void cancelNextFutureEvent(Predicate<SimEvent> filter) {
		Iterator<SimEvent> it = future.iterator();
		while (it.hasNext()) {
			SimEvent ev = it.next();
			if(filter.test(ev)) {
				it.remove();
				break;
			}
		}
	}
	
	/**
	 * Runs the simulation until completion.
	 */
	public synchronized final void run() {
		checkState(status.nextState().contains(Status.RUNNING), 
				"Cannot run a simulation at state %s", status);
		
		onConfigure();
		startSimulation();
		if (processEvents()) {
			shutdownSimulation();
		}
	}	
	
	/**
	 * Pauses a running simulation
	 */
	public synchronized void pause() {
		checkState(status.nextState().contains(Status.PAUSED), 
				"Cannot pause simulation at state %s", status);

		setStatus(Status.PAUSED);
	}
	
	/**
	 * Resumes a previously started simulation
	 */
	public synchronized void resume() {
		checkState(status.nextState().contains(Status.RUNNING), 
				"Cannot resume simulation at state %s", status);

		setStatus(Status.RUNNING);
		if (processEvents()) {
			shutdownSimulation();
		}
	}
		
	private void startSimulation() {
		log.info("Starting the simulation...");		
		setStatus(Status.RUNNING);
		startEntities();
	}
	
	private void shutdownSimulation() {
		stopEntities();
		setStatus(Status.COMPLETE);
		log.info("The simulation is complete...");
	}
	
	/* Processes the simulation events. */
	private boolean processEvents() {
		while (!isPaused() && (clock.getTime() < this.timeSpan || ! this.abruptInterrupt)) {
			// if the future event queue is empty, then stop
			// the simulation
			if (runClockTick()) {
				return true;
			}
		}
		
		return !isPaused() && this.abruptInterrupt ? true : false;
	}
	
	/**
	 * Returns the current simulation clock time.
	 * @return the current simulation clock time.
	 */
	public long currentTime() {
		return clock.getTime();
	}
		
	// --- package level methods ---
	
	// used by an entity to send an event to another
	@SuppressWarnings("rawtypes")
	void send(int src, int dest, long delay, Enum type, Object content) {
		checkArgument(delay >= 0L, "Send delay must be >= 0.");
		future.add(new SimEvent(type, content, clock.getTime() + delay, src, dest));
	}
	
	/* 
	 * Where the magic of the simulation lies. It executes all the events
	 * scheduled for one tick of the simulation clock
	 */
	private boolean runClockTick() {
		boolean queueEmpty = false;
		
		if (this.eventPriority != null) {
			Collections.sort(deferred, this.eventPriority);
		}
		
		Iterator<SimEvent> it = deferred.iterator();
		while (!isPaused() && it.hasNext()) {
			SimEvent ev = it.next();
			SimEntity targetEnt = entities.getEntity(ev.destination());
			if (targetEnt.isEnabled()) {
				targetEnt.process(ev);
			} 
			it.remove();
		}
		
		if (isPaused()) {
			return queueEmpty;
		}
		
		// If there are more future events then deal with them
		if (future.size() > 0) {
			it = future.iterator();
			SimEvent first = it.next();
			processEvent(first);
			it.remove();
			
			// Check if next events occur at same time...
			boolean tryMore = it.hasNext();
			while (tryMore) {
				SimEvent next = it.next();
				if (next.time() == first.time()) {
					processEvent(next);
					it.remove();
					tryMore = it.hasNext();
				} else {
					tryMore = false;
				}
			}
		} else { 
			queueEmpty = true;
		}
		
		return queueEmpty;
	}
	
	// processes a simulation event
	private void processEvent(SimEvent ev) {
		int dest;
		// Update the system's clock
		if (ev.time() < clock.getTime()) {
			throw new IllegalArgumentException("The event was scheduled for the past.");
		}
		clock.setTime(ev.time());
		
		// process the event
		dest = ev.destination();
		if (dest < 0) {
			throw new IllegalArgumentException("Illegal target entity: " + dest);
		} else {
			deferred.add(ev);
		}
	}
	
	// helper method to start the entities
	private void startEntities() {
		for(SimEntity ent : entities) {
			ent.onStart();
		}
	}
	
	// helper method to shut down the entities
	private void stopEntities() {
		for(SimEntity ent : entities) {
			ent.onShutdown();
		}
	}

	/**
	 * Interface that a state transition must implement.
	 * @param <T>
	 */
	interface StateTransition<T> {
		/**
		 * Returns the next state.
		 * @return the next state given a current state.
		 */
		List<T> nextState();
	}
	
	/**
	 * Possible simulation statuses
	 */
	public enum Status implements StateTransition<Status> {
		/** Simulation is not started */
		NOT_STARTED {
			public List<Status> nextState() {
				return Arrays.asList(RUNNING);
			}
		},
		
		/** Simulation is running */
		RUNNING {
			public List<Status> nextState() {
				return Arrays.asList(PAUSED, COMPLETE);
			}
		},
		/** Simulation is paused */
		PAUSED {
			public List<Status> nextState() {
				return Arrays.asList(RUNNING);
			}
		},
		/** Simulation is finished */
		COMPLETE {
			public List<Status> nextState() {
				return Arrays.asList(NOT_STARTED);
			}
		}
	}

	/* 
	 * Class to which entity management is delegated. This is to avoid that users
	 * mess up with the number of entities in the system and how they get created or deleted.
	 * So the whole thing must be managed by Simulation class.
	 */
	private class EntityList extends ArrayList<SimEntity> {
		private static final long serialVersionUID = 6922342328272754582L;

		// adds an entity to the simulation
		boolean addEntity(SimEntity entity) {
			return super.add(entity);
		}
		
		// returns an array of entities that match the filtering criteria
		List<SimEntity> getEntities(Predicate<SimEntity> filter) {
			return filter(this, filter);
		}
		
		// To search an entity by its name
		SimEntity getEntity(String name) {
			for (SimEntity entity : this) {
				if (entity.getName().equals(name)) {
					return entity;
				}
			}
			return null;
		}
		
		// To search an entity by its id
		SimEntity getEntity(int id) {
			for (SimEntity entity : this) {
				if (entity.getId() == id) {
					return entity;
				}
			}
			return null;
		}
		
		private List<SimEntity> filter(List<SimEntity> thelist, Predicate<SimEntity> p) {
			 List<SimEntity> result = new ArrayList<>();
			 for (SimEntity e : thelist) {
				 if (p.test(e)) {
					 result.add(e);
			     }
			 }
			 
			 return result;
		}
	}
}