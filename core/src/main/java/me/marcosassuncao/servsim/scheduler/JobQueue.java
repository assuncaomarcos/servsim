package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.job.Job;

import java.util.Optional;
import java.util.TreeSet;

/**
 * This class defines the basic behavior of a job queue.
 *
 * @author Marcos Dias de Assuncao
 */
public class JobQueue extends TreeSet<Job> {

    /**
     * Searches a job by its id.
     * @param jobId the job id.
     * @return an {@link Optional<Job>}
     */
    public Optional<Job> getJob(int jobId) {
        return this.stream()
                .filter(j -> j.getId() == jobId)
                .findFirst();
    }

    /**
     * Searches and cancels a job by its id.
     * @param jobId the job id.
     * @return an {@link Optional<Job>}, which might
     * not be present if the job was not found in this queue.
     */
    public Optional<Job> removeJob(int jobId) {
        Optional<Job> job = getJob(jobId);
        job.ifPresent(super::remove);
        return job;
    }
}
