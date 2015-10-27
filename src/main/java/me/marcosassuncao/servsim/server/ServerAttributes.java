package me.marcosassuncao.servsim.server;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * This class represents the attributes of a given server. That is, the
 * list of resources that it has, their availability, etc.
 * 
 * @see Server
 * 
 * @author Marcos Dias de Assuncao
 */

public class ServerAttributes {
	private int serverId = -1;
	private ResourcePool resources;
	private ServerAvailability availability;

	/**
	 * Creates a new server attribute object
	 * @param res the resource list
	 * @param avail the availability information
	 */
	public ServerAttributes(ResourcePool res, 
			ServerAvailability avail) {
		this.resources = checkNotNull(res, "Resource pool cannot be null");
		this.availability = avail;
	}
	
	void setServerId(int id) {
		checkArgument(id != -1, "Invalid server Id");
		this.serverId = id;
	}
	
	/**
	 * Returns the server id
	 * @return the server id
	 */
	public int getServerId() {
		return serverId;
	}
	
	/**
	 * Returns the resource pool
	 * @return the resource pool
	 */
	public ResourcePool getResourcePool() {
		return resources;
	}
	
	/**
	 * Sets a resource pool.
	 * @param pool the provided pool
	 */
	public void setResourcePool(ResourcePool pool) {
		this.resources = pool;
	}
	
	/**
	 * Gets the resource availability information
	 * @return the resource availability information
	 */
	public ServerAvailability getResourceAvailability() {
		return availability;
	}
	
	/**
	 * Sets the resource availability
	 * @param avail an object specifying the resource availability
	 */
	public void setResourceAvailability(ServerAvailability avail) {
		this.availability = avail;
	}
}
