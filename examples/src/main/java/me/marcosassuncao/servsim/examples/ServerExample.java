package me.marcosassuncao.servsim.examples;

import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.server.Server;
import me.marcosassuncao.servsim.server.ServerUser;

/**
 * This example illustrates how to create a server and a user that sends
 * a number of jobs to be executed by the server.
 */
class User extends ServerUser {
    private int numJobs;
    private long interval;
    private int serverId;

    public User(String name, int serverId,
                long interval, int numJobs) throws
            IllegalArgumentException {
        super(name);
        this.numJobs = numJobs;
        this.interval = interval;
        this.serverId = serverId;
    }

    @Override
    public void onStart() {
        for (int i = 1; i <= numJobs; i++) {
            int duration = 5; // job duration is 5 time units (seconds)
            Job j = new Job(duration);
            super.submitJob(serverId, i * this.interval, j);
        }
    }

    @Override
    public void onJobReceived(int sourceId, Job job) {
        System.out.println("Received job " + job.getId() + " from " +
                sourceId + " at " + super.currentTime());
    }
}

public class ServerExample {

    public static final void main(String[] args) {
        Simulation sim = new Simulation() {

            @Override
            public void onConfigure() {
                long interval = 5;       // job inter-arrival time in seconds
                int numberJobs = 3;      // total number of jobs
                int serverCapacity = 10; // The server has 10 CPUs/resources

                Server server = Server.builder()
                        .setName("Server")
                        .setCapacity(serverCapacity)
                        .build();

                User user = new User("User-1", server.getId(),
                        interval, numberJobs);

                registerEntity(server);
                registerEntity(user);
            }
        };

        sim.run();
    }
}
