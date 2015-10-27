package me.marcosassuncao.servsim.server;

import java.util.TimeZone;

/**
 * This interface for resource availability information. Resource availability
 * defines when and what percentage of resources will be available at a server. 
 * This is used by scenarios that consider that the resource availability 
 * information may change over time due to resources joining or leaving a pool.
 * 
 * @author Marcos Dias de Assuncao
 * 
 * @see ServerHourlyAvailability
 */

public interface ServerAvailability {

	/**
	 * Returns the time zone used by this availability object
	 * @return the time zone used by this availability object
	 * @see TimeZone
	 */
	default TimeZone getTimeZone() {
		return TimeZone.getDefault();
	}
	
	/**
	 * Returns the availability at the current simulation time
	 * @return a value between (0 and 1) representing the availability
	 */
	default float getAvailability() {
		return 1f;
	}
}
