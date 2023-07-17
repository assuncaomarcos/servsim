package me.marcosassuncao.servsim.examples;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;

/**
 * This example creates two entities (i.e. PingEntity and PongEntity) that
 * respectively send ping messages and respond with a pong. Two event types
 * are used to indicate the ping and pong messages, namely {@link SimEvent
 * .Type#TASK_ARRIVE} and {@link SimEvent.Type#TASK_COMPLETE}.
 *
 * @see SimEvent.Type
 */
public class PingExample {

    public static void main(final String[] args) {
        Simulation sim = new Simulation() {
            @Override
            public void onConfigure() {
                long interval = 5;
                int numberPing = 3;

                PongEntity pong = new PongEntity("Pong");
                PingEntity ping = new PingEntity("Ping", pong.getId(),
                        interval, numberPing);
                registerEntity(pong);
                registerEntity(ping);
            }
        };
        sim.run();
    }
}

/**
 * Simple entity that receives a task and returns it to sender.
 */
class PongEntity extends SimEntity {

    PongEntity(final String name) throws IllegalArgumentException {
        super(name);
    }

    @Override
    public void onStart() { }

    @Override
    public void process(final SimEvent ev) {
        if (ev.type() == SimEvent.Type.TASK_ARRIVE) {
            // send pong
            System.out.println("Received ping, sending pong...");
            super.send(ev.source(), SimEvent.SEND_NOW,
                    SimEvent.Type.TASK_COMPLETE, null);
        }
    }

    @Override
    public void onShutdown() { }
}

/**
  * Simple entity that sends tasks to another entity.
  */
class PingEntity extends PongEntity {

    /** ID of the destination entity. */
    private final int dstEntity;

    /** Time interval between messages/events. */
    private final long interval;

    /** Number of messages to send. */
    private final int numberPing;

    PingEntity(final String name, final int dstEntity,
               final long interval, final int numberPing)
            throws IllegalArgumentException {
        super(name);
        this.dstEntity = dstEntity;
        this.interval = interval;
        this.numberPing = numberPing;
    }

    @Override
    public void onStart() {
        for (int i = 1; i <= numberPing; i++) {
            super.send(dstEntity, interval * i,
                    SimEvent.Type.TASK_ARRIVE, null);
        }
    }
}
