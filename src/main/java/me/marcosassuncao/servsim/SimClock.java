package me.marcosassuncao.servsim;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * It represents the simulation clock, time unit used, etc.
 * 
 * @author Marcos Dias de Assuncao
 */

public class SimClock {
	private long time = 0L;
	private TimeUnit timeUnit = TimeUnit.SECONDS;
	private Date startDate; // date at which the simulation started

	/**
	 * Package level constructor
	 */
	SimClock() {  
		startDate = new Date();
	}
	
	/**
	 * Sets the time unit to be used for simulation
	 * @param unit the time unit
	 */
	void setUnit(TimeUnit unit) {
		this.timeUnit = unit;
	}
	
	/**
	 * Sets the simulation time
	 * @param time the current simulation time
	 */
	void setTime(long time) {
		this.time = time;
	}
	
	/**
	 * Resets the simulation clock 
	 */
	void reset() {
		time = 0L;
		startDate = new Date();
		timeUnit = TimeUnit.SECONDS;
	}
	
	/**
	 * Returns the simulation time
	 * @return the simulation time
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Gets the time unit used for simulation
	 * @return the time unit used for simulation
	 */
	public TimeUnit getUnit() {
		return timeUnit;
	}
	
	/**
	 * In some simulations, it might be important to specify a date that 
	 * represents the start time of the simulation. This information can be
	 * used to control, for example, the load or availability of resources
	 * during days of the week.
	 * @param date the start date of the simulation
	 * @throws IllegalArgumentException if {@code date} is {@code null}
	 */
	void setStartDate(Date date) {
		this.startDate = checkNotNull(date);
	}
	
	/**
	 * The date object that represents the start time of this simulation.
	 * @return date object that represents the start time of this simulation.
	 */
	public Date getStartDate() {
		return this.startDate;
	}
	
	/**
	 * Returns the current simulation date by adding the simulation clock
	 * to the date at which the simulation started.
	 * @return a date object that indicates the date that the 
	 * current simulation clock represents
	 */
	public Date getCurrentDate() {
		return new Date(startDate.getTime() + timeUnit.toMillis(time));
	}
}