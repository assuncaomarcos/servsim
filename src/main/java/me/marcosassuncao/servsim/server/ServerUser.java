package me.marcosassuncao.servsim.server;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.WorkUnit;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_ARRIVE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_CANCEL;

/**
 * Entity represents a user who sends jobs to another entity.
 *
 * @author Marcos Dias de Assuncao
 */

public abstract class ServerUser extends SimEntity {

    /**
     * Creates a user of the server infrastructure.
     * @param name the entity name.
     * @throws IllegalArgumentException if the name is not provided.
     */
    public ServerUser(final String name) throws IllegalArgumentException {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final SimEvent ev) {
        if (ev.type() == SimEvent.Type.RESULT_ARRIVE) {
            onJobReceived(ev.source(), (Job) ev.content());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() { }

    @Override
    public void onShutdown() { }

    /**
     * Called when entity received a job back from a server.
     * @param src the source of the job received
     * @param job the job received
     */
    public abstract void onJobReceived(int src, Job job);

    /**
     * Cancels a job in execution.
     * @param dst the id of the destination entity
     * @param jobId the Id of the job to be cancelled
     */
    public void cancelJob(final int dst, final int jobId) {
        checkArgument(dst >= 0,
                "Invalid entity Id");
        checkArgument(jobId >= 0,
                "Invalid job Id");
        super.send(dst, SimEvent.SEND_NOW, TASK_CANCEL, jobId);
    }

    /**
     * Sends a job to a given entity, such as a server.
     * @param dst the id of the destination entity
     * @param delay the delay for sending the job
     * @param job the job to be sent
     */
    public void submitJob(final int dst,
                          final long delay,
                          final Job job) {
        checkNotNull(job);
        checkArgument(dst >= 0,
                "Invalid entity Id");
        if (job.getOwnerEntityId() == WorkUnit.ID_NOT_SET) {
            job.setOwnerEntityId(this.getId());
        }
        super.send(dst, delay, TASK_ARRIVE, job);
    }
}
