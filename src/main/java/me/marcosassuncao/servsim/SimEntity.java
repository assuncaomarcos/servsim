package me.marcosassuncao.servsim;

import java.util.Date;

import me.marcosassuncao.servsim.event.EventSink;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class represents a simulation entity that handles events and that can
 * send events to other entities. When this class is extended, there are a few
 * methods that must be implemented:
 * <ul>
 *  <li> {@link #onStart()} is invoked by {@link Simulation} when the simulation 
 *  starts. This is the place where the initial creation of events must be performed. For
 *  example, if the entity reads a workload from a file, reading of the workload file 
 *  and the creation of simulation events will have to be triggered by this method.
 *  <li> {@link #process(SimEvent)} is invoked by {@link Simulation}
 *  class whenever there is an event that needs to be processed by the entity.
 *  <li> {@link #onShutdown()} is invoked by {@link Simulation} before the
 *  simulation finishes. If you want to save data in log files this is the method
 *  in which the corresponding code would be placed.
 * </ul>
 * 
 * @author Marcos Dias de Assuncao
 * 
 */
public abstract class SimEntity implements EventSink<SimEvent> {
	// Used to generate a single ID to the entity
	private static int nextId = 0;
	private final String name;
	private final int id;
	private boolean enabled = true;
	private Simulation sim;
	
	/**
	 * Creates a new simulation entity.
	 * @param name the entity's name
	 * @throws IllegalArgumentException if the name is <code>null</code> or has size 0.
	 */
	public SimEntity(String name) throws IllegalArgumentException {
		this.name = checkNotNull(name);
		this.id = createId();
		enableEntity();
	}
		
	/**
	 * Sets the simulation object to which the 
	 * entity is registered
	 * @param sim the simulation object
	 */
	protected void setSimulation(Simulation sim) {
		this.sim = sim;
	}
	
	/**
	 * Gets the simulation object to which the 
	 * entity is registered
	 * @return the {@link Simulation} object
	 */
	protected Simulation getSimulation() {
		return sim;
	}

	/**
	 * Gets the name of the entity
	 * @return the name of the entity
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the entity's id
	 * @return the entity's id
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Checks whether the entity is enabled.
	 * @return <code>true if the entity is enabled</code>; <code>false</code> otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Disables the entity
	 */
	public void disableEntity() {
		this.enabled = false;
	}
	
	/**
	 * Enables the entity
	 */
	public void enableEntity() {
		this.enabled = true;
	}
	
	/**
	 * This method is invoked by the {@link Simulation} when the simulation starts. 
	 */
	public abstract void onStart();
	
	/**
	 * This method is invoked by the {@link Simulation}
	 * class whenever there is an event in the deferred queue, which needs to be
	 * processed by the entity. 
	 * @param ev the event to be processed by the entity
	 */
	public abstract void process(SimEvent ev);
	
	/**
	 * This method is invoked by the {@link Simulation} before the
	 * simulation finishes. If you want to save data in log files this is the method
	 * in which the corresponding code would be placed. 
	 */
	public abstract void onShutdown();
	
	/**
	 * Used by the entity to send a message (i.e. create an event) to (for) another entity
	 * @param dest the id of the target entity
	 * @param delay how many clock units from now this event will be handled
	 * @param type the type of event to be sent
	 * @param content the content associated with the event
	 */
	@SuppressWarnings("rawtypes")
	protected void send(int dest, long delay, Enum type, Object content) {
		getSimulation().send(this.id, dest, delay, type, content);
	}
	
	/**
	 * Returns the current simulation clock time.
	 * @return the current simulation clock time.
	 */
	protected long currentTime() {
		return sim.currentTime();
	}
	
	/**
	 * Gets the current simulation date
	 * @return the current simulation date
	 */
	protected Date currentDate() {
		return sim.getClock().getCurrentDate();
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			  .add("id", id)
			  .add("name", name)
			  .add("enabled", enabled)
			  .toString();
	}
	
	/* synchronised ID creation */
	private static synchronized int createId() {
		return ++nextId;
	}
}
