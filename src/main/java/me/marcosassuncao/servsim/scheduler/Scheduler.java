package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.event.EventSink;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.server.ServerAttributes;

/**
 * Common interface that all schedulers must implement in order
 * to be used by {@link me.marcosassuncao.servsim.server.Server}.
 *
 * @author Marcos Dias de Assuncao
 */

public interface Scheduler extends EventSink<SimEvent> {

    /**
     * Method to handle the job arrival.
     * @param job the job
     */
    void doJobProcessing(Job job);

    /**
     * Method to handle the completion of a job.
     * @param job the job
     */
    void doJobCompletion(Job job);

    /**
     * Method to handle the cancellation of a job.
     * @param id the job
     */
    void doJobCancel(int id);

    /**
     * Initialise the scheduling policy.
     * @param attr the server's attributes
     */
    void initialize(ServerAttributes attr);

}
