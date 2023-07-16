package me.marcosassuncao.servsim.server;

import me.marcosassuncao.servsim.event.AbstractEvent;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.ResourcePool.ResourceStatus;

/**
 * An event triggered by a change in range of resources.
 *
 * @author Marcos Dias de Assuncao
 */

public class ResourceStatusEvent extends
        AbstractEvent<ResourceStatus, RangeList> {

    /**
     * Construct a resource event.
     * @param type the event type.
     * @param subject the subject to use.
     * @param time the time the event is triggered.
     */
    protected ResourceStatusEvent(final ResourceStatus type,
                                  final RangeList subject,
                                  final long time) {
        super(type, subject, time);
    }
}
