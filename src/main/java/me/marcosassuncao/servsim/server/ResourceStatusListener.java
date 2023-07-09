package me.marcosassuncao.servsim.server;

import me.marcosassuncao.servsim.event.EventListener;

/**
 * Interface that must be implemented by classes that are interested
 * in changes in the status of resources.
 *
 * @author Marcos Dias de Assuncao
 *
 * @see me.marcosassuncao.servsim.server.ResourcePool.ResourceStatus
 */

public interface ResourceStatusListener
        extends EventListener<ResourceStatusEvent> {

}
