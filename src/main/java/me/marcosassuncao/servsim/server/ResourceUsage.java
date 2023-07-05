package me.marcosassuncao.servsim.server;

/**
 * Class used to return the results of a query to check
 * the resource usage of a given resource profile.
 * 
 * @author Marcos Dias de Assuncao
 */

public class ResourceUsage {
	private long time;
	private int numResources;

	/**
	 * Creates a new usage object
	 * @param time the time associated with the object
	 * @param numResources the number of resources in use at <code>time</code>
	 */
	public ResourceUsage(long time, int numResources) {
		this.time = time;
		this.numResources = numResources;
	}
	
	/**
	 * Gets the time associated with this object
	 * @return the time associated with this object
	 */
	public long getTime() {
		return this.time;
	}
	
	/**
	 * Gets the number of resources in use
	 * @return the number of resources in use
	 */
	public int getNumResources() {
		return this.numResources;
	}
}
