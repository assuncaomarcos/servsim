package me.marcosassuncao.servsim.scheduler;

import me.marcosassuncao.servsim.job.Reservation;

/**
 * Common interface that schedulers that enable reservations must
 * implement in order to be used by
 * {@link me.marcosassuncao.servsim.server.Server}.
 *
 * @author Marcos Dias de Assuncao
 */

public interface ReservationScheduler extends Scheduler {

    /**
     * Method to handle a reservation request.
     * @param r the resource reservation
     */
    void doReservationProcessing(Reservation r);

    /**
     * Method to handle the completion of a reservation.
     * @param r the resource reservation
     */
    void doReservationCompletion(Reservation r);

    /**
     * Method to handle the cancellation of a reservation.
     * @param id the Id of the resource reservation
     */
    void doReservationCancel(int id);

}
