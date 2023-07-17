package me.marcosassuncao.servsim.examples;

import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.scheduler.ConsBackfillScheduler;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.ServerUser;

import java.util.UUID;

/**
 * This example creates a server with a parallel,
 * conservative backfilling scheduler, and a user entity
 * that sends 100 jobs to the server for execution.
 */
public class ConsBackfillingExample {

    public static void main(final String[] args) {
        int capacity = 10;
        Server srv = Server.builder()
                .setName("Server-" + UUID.randomUUID())
                .setScheduler(new ConsBackfillScheduler())
                .setCapacity(capacity).build();

        int jobDuration = 100;
        int numJobs = 100;

        // Creates the user entity
        ParallelUser user =
                new ParallelUser(srv.getId(), numJobs, jobDuration);

        // Simulation trigger class
        Simulation sim = new Simulation() {
            @Override
            public void onConfigure() {
                super.registerEntity(srv);
                super.registerEntity(user);
            }
        };

        sim.run();
    }
}

/**
 * The user entity which issues the jobs.
 */
class ParallelUser extends ServerUser {

    /** The server entity's id. */
    private final int serverId;

    /** The number of jobs to create. */
    private final int numJobs;

    /** The duration of a single job. */
    private final int jobDuration;

    /**
     * Creates a user of the server infrastructure.
     *
     * @param serverId the id of the server entity
     * @param numJobs the number of jobs to issue
     * @param jobDuration the duration of a single job in seconds
     */
    ParallelUser(final int serverId,
                 final int numJobs,
                 final int jobDuration) {
        super("User-" + UUID.randomUUID());
        this.serverId = serverId;
        this.numJobs = numJobs;
        this.jobDuration = jobDuration;
    }

    @Override
    public void onStart() {
        for (int i = 0; i < numJobs; i++) {
            Job j = new Job(jobDuration, 5);
            super.submitJob(serverId, SimEvent.SEND_NOW, j);
        }
    }

    @Override
    public void onJobReceived(final int src, final Job job) {
        System.out.println(job);
    }
}
