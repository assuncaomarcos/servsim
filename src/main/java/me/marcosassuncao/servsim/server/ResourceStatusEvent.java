package me.marcosassuncao.servsim.server;

import me.marcosassuncao.servsim.event.AbstractEvent;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool.ResourceStatus;

/**
 * An event triggered by a change in range of resources.
 * 
 * @author Marcos Dias de Assuncao
 */

public class ResourceStatusEvent extends AbstractEvent<ResourceStatus, RangeList> {
	
	protected ResourceStatusEvent(ResourceStatus type, RangeList subject, long time) {
		super(type, subject, time);
	}
}