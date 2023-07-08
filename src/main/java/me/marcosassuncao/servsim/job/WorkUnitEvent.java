package me.marcosassuncao.servsim.job;

import me.marcosassuncao.servsim.event.AbstractEvent;

import java.util.Optional;

/**
 * An event triggered by a change in a work unit.
 *
 * @author Marcos Dias de Assuncao
 */

public class WorkUnitEvent extends
        AbstractEvent<WorkUnitEvent.Type, DefaultWorkUnit> {
    /**
     * The previous status of the work unit.
     */
    private final Optional<WorkUnit.Status> prevStatus;

    /**
     * Creates a new job event.
     * @param type the type of event
     * @param subject the job associated with the event
     * @param time the time at which the event occurred
     */
    public WorkUnitEvent(final long time,
                         final Type type,
                         final DefaultWorkUnit subject) {
        this(type, subject, time, null);
    }

    /**
     * Creates a new job event.
     * @param type the type of event
     * @param subject the job associated with the event
     * @param time the time at which the event occurred
     * @param prevStatus the status of the job prior to the
     *                   occurrence of the event
     */
    public WorkUnitEvent(final Type type,
                         final DefaultWorkUnit subject,
                         final long time,
                         final WorkUnit.Status prevStatus) {
        super(type, subject, time);
        this.prevStatus = Optional.ofNullable(prevStatus);
    }

    /**
     * Returns the status of the job previous to
     * the occurrence of the event.
     * @return the previous job status
     */
    public WorkUnit.Status previousStatus() {
        return prevStatus.orElse(WorkUnit.Status.UNKNOWN);
    }

    /**
     * Types of job events.
     */
    public enum Type {
        /** The status of a job has changed. */
        STATUS_CHANGED
    }
}
