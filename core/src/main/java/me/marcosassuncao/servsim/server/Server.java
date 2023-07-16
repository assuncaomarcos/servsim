package me.marcosassuncao.servsim.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static me.marcosassuncao.servsim.SimEvent.Type.RESERVATION_CANCEL;
import static me.marcosassuncao.servsim.SimEvent.Type.RESERVATION_REQUEST;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_ARRIVE;
import static me.marcosassuncao.servsim.SimEvent.Type.TASK_CANCEL;

import java.util.UUID;

import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;
import me.marcosassuncao.servsim.event.EventListener;
import me.marcosassuncao.servsim.job.Job;
import me.marcosassuncao.servsim.job.Reservation;
import me.marcosassuncao.servsim.job.WorkUnitEvent;
import me.marcosassuncao.servsim.scheduler.AbstractScheduler;
import me.marcosassuncao.servsim.scheduler.DefaultScheduler;
import me.marcosassuncao.servsim.scheduler.ReservationScheduler;
import me.marcosassuncao.servsim.scheduler.Scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is a server that handles work units. The idea is that the
 * server will provide the basic behaviour required of an entity that receives
 * and processes work units.
 *
 * @see SimEntity
 *
 * @author Marcos Dias de Assuncao
 */

public class Server extends SimEntity {
    /** Default logger. */
    private static final Logger LOGGER =
            LogManager.getLogger(Server.class.getName());

    /** The server attributes. */
    private final ServerAttributes attributes;

    /** The scheduler used for job scheduling. */
    private final Scheduler scheduler;

    /**
     * Creates a new server.
     * @param name the server's name
     * @param attrib the server's attributes
     * @param scheduler the server's scheduling policy
     * @throws IllegalArgumentException if <code>name</code>,
     * <code>attrib</code> or <code>policy</code> are <code>null</code>
     */
    public Server(final String name,
                  final ServerAttributes attrib,
                  final Scheduler scheduler) {
        super(name);
        this.attributes = checkNotNull(attrib,
                "Server attributes cannot be null");
        this.scheduler = checkNotNull(scheduler,
                "Policy cannot be null");
        this.attributes.setServerId(super.getId());

        // initialises the policy
        this.scheduler.initialize(this.attributes);
    }

    /**
     * Returns the server's attributes used by the scheduler.
     * @return the server's attributes
     */
    public ServerAttributes getServerAttributes() {
        return attributes;
    }

    /**
     * Returns the scheduling policy used by this server.
     * @return the scheduling policy used by this server
     */
    public Scheduler getSchedulingPolicy() {
        return scheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final SimEvent ev) {
        if (ev.type() == TASK_ARRIVE) {
            try {
                Job job = (Job) ev.content();
                // sets the unit's submission time.
                job.setSubmitTime(super.currentTime());
                scheduler.doJobProcessing(job);
            } catch (ClassCastException cce) {
                LOGGER.error("Invalid job received for processing.");
            }
        }  else if (ev.type() == TASK_CANCEL) {
            try {
                int id = (Integer) ev.content();
                scheduler.doJobCancel(id);
            } catch (ClassCastException cce) {
                LOGGER.error("Invalid job id sent for cancellation.");
            }
        } else if (ev.type() == RESERVATION_REQUEST) {
            if (this.scheduler instanceof ReservationScheduler) {
                try {
                    ReservationScheduler sched =
                            (ReservationScheduler) scheduler;
                    Reservation r = (Reservation) ev.content();
                    // sets the unit's submission time.
                    r.setSubmitTime(super.currentTime());
                    sched.doReservationProcessing(r);
                } catch (ClassCastException cce) {
                    LOGGER.error("Invalid reservation sent for processing.");
                }
            } else {
                LOGGER.error("Scheduler cannot handle reservation request");
            }
        } else if (ev.type() == RESERVATION_CANCEL) {
            if (this.scheduler instanceof ReservationScheduler) {
                ReservationScheduler sched =
                        (ReservationScheduler) scheduler;
                try {
                    int id = (Integer) ev.content();
                    sched.doReservationCancel(id);
                } catch (ClassCastException cce) {
                    LOGGER.error(
                            "Invalid reservation id sent for cancellation.");
                }
            } else {
                LOGGER.error("Scheduler cannot handle reservation request");
            }
        } else {
            scheduler.process(ev);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setSimulation(final Simulation sim) {
        super.setSimulation(sim);
        sim.registerEntity(attributes.getResourcePool());
        sim.registerEntity((SimEntity) scheduler);
    }

    /**
     * Returns a new server builder.
     * @return a new server builder
     */
    public static Server.Builder builder() {
        return new Server.Builder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onShutdown() { }

    /**
     * Builder for constructing a new server object.
     */
    public static final class Builder {
        /** Server name. */
        private String name;

        /** Server scheduler. */
        private AbstractScheduler sched;

        /** Resource pool. */
        private ResourcePool pool;

        /** Availability data structure. */
        private ServerAvailability avail;

        /** Server maximum capacity (number of processors). */
        private int capacity = -1;

        /** The listeners to job events. */
        private EventListener<WorkUnitEvent> listener;

        /**
         * Sets the server name.
         * @param name the server name
         * @return the builder
         */
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the scheduler to be used.
         * @param scheduler the scheduler instance
         * @return the builder
         */
        public Builder setScheduler(
                final AbstractScheduler scheduler) {
            this.sched = scheduler;
            return this;
        }

        /**
         * Sets the event listener for events triggered by the scheduler.
         * @param listener the listener to be registered with the scheduler
         * @return the builder
         */
        public Builder setWorkUnitEventListener(
                final EventListener<WorkUnitEvent> listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Sets the resource pool.
         * @param pool the resource pool
         * @return the builder
         */
        public Builder setResourcePool(
                final ResourcePool pool) {
            this.pool = pool;
            return this;
        }

        /**
         * Sets the resource availability scheme.
         * @param avail the resource availability scheme
         * @return the builder
         */
        public Builder setResourceAvailability(
                final ServerAvailability avail) {
            this.avail = avail;
            return this;
        }

        /**
         * Sets the resource capacity.
         * @param capacity the resource capacity
         * @return the builder
         */
        public Builder setCapacity(final int capacity) {
            checkArgument(capacity > 0,
                    "Capacity must be >= 0");
            this.capacity = capacity;
            return this;
        }

        /**
         * Builds the server instance.
         * @return the server instance
         */
        public Server build() {
            if (name == null) {
                name = "Server-" + UUID.randomUUID();
            }
            if (sched == null) {
                sched = new DefaultScheduler(name + "_Scheduler");
            }
            if (avail == null) {
                avail = new ServerAvailability() { };
            }
            if (pool == null) {
                if (capacity == -1) {
                    capacity = 1;
                }
                pool = new DefaultResourcePool(capacity);
            }
            if (listener != null) {
                sched.addListener(listener);
            }

            ServerAttributes att = new ServerAttributes(pool, avail);
            return new Server(name, att, sched);
        }
    }
}
