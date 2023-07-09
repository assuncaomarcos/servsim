package me.marcosassuncao.servsim.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import me.marcosassuncao.servsim.SimClock;

/**
 * This class implements the information about the resource availability.
 * It defines the percentage of resources that should be available at
 * a given moment, and when will be the next event change.
 * <p>
 * <b>NOTE:</b> By default, this availability object assumes that
 * all resources should be available during the whole simulation time. This
 * class allows one to set the availability of resources over a week at
 * a granularity of one hour.
 *
 * @author Marcos Dias de Assuncao
 */

public class ServerHourlyAvailability implements ServerAvailability {

    /** Stores the availability for all hours of the week. */
    private final float[] avail;

    /** The time zone to use. */
    private final TimeZone timeZone;

    /** Reference to the simulation clock. */
    private final SimClock clock;

    /**
     * Creates a new availability object with the host's time zone.
     * @param clock the simulation clock to use
     * @see TimeZone#getDefault()
     */
    public ServerHourlyAvailability(final SimClock clock) {
        this(clock, TimeZone.getDefault());
    }

    /**
     * Creates a new availability object.
     * @param clock the simulation clock to use
     * @param timeZone the time zone used by this resource availability object
     * @see TimeZone
     */
    public ServerHourlyAvailability(final SimClock clock,
                                    final TimeZone timeZone) {
        this.clock = checkNotNull(clock);
        avail = new float[7 * 24];
        Arrays.fill(avail, 1f);
        this.timeZone = timeZone;
    }

    /**
     * Returns the time zone used by this availability object.
     * @return the time zone used by this availability object
     * @see TimeZone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /* Test if integer is a valid week day */
    private boolean validWeekDay(final int weekday) {
        return weekday >= Calendar.SUNDAY && weekday <= Calendar.SATURDAY;
    }

    /* Test if hour is valid */
    private boolean validHour(final int hour) {
        return hour >= 0 && hour <= 23;
    }

    /**
     * Sets the availability during a given period.
     * @param dStart the start day of the week (between {@link Calendar#SUNDAY}
     * and {@link Calendar#SATURDAY}).
     * @param hStart the start our of the day (between 0 and 23)
     * @param dEnd the end day of the week (between {@link Calendar#SUNDAY}
     * and {@link Calendar#SATURDAY}).
     * @param hEnd the end our of the day (between 0 and 23)
     * @param av the availability (between 0 and 1
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     */
    public void setAvailability(final int dStart,
                                final int hStart,
                                final int dEnd,
                                final int hEnd,
                                final float av) {
        checkArgument(validWeekDay(dStart),
                "Invalid start of week: %s", dStart);
        checkArgument(validWeekDay(dEnd),
                "Invalid end of week: %s", dEnd);
        checkArgument(validHour(hStart),
                "Start hour must be >= 0 and <= 23");
        checkArgument(validHour(hEnd),
                "End hour must be >= 0 and <= 23");
        checkArgument(dStart != dEnd && hEnd > hStart,
                "End hour must be > than start hour");
        checkArgument(Float.compare(av, 0f) >= 0
                        && Float.compare(av, 1f) <= 0,
                "Availability bust be >= 0 or <= 1");

        // New end day if got in the middle of a week
        int nDEnd = dEnd < dStart ? dEnd + 7 : dEnd;

        if (dStart == nDEnd) {
            for (int h = hStart; h <= hEnd; h++) {
                avail[(dStart - 1) * 24 + h] = av;
            }
        } else {
            for (int h = hStart; h < 24; h++) {
                avail[(dStart - 1) * 24 + h] = av;
            }

            for (int h = 0; h <= hEnd; h++) {
                avail[(nDEnd % 7 - 1) * 24 + h] = av;
            }

            for (int d = dStart; d < nDEnd - 1; d++) {
                for (int h = 0; h < 24; h++) {
                    avail[d % 7 * 24 + h] = av;
                }
            }
        }
    }

    /**
     * Returns the availability at the week day and hour.
     * @param weekDay the day of the week, such that
     * {@link Calendar#SUNDAY} &gt;= weekDay &lt;= {@link Calendar#SATURDAY}
     * @param hour hour of the day, from 0 to 23
     * @return a value between (0 and 1) representing the availability
     */
    public float getAvailability(final int weekDay, final int hour) {
        checkArgument(validWeekDay(weekDay),
                "Invalid day of week");
        checkArgument(validHour(hour),
                "Invalid hour: %s", hour);
        return avail[(weekDay - 1) * 24 + hour];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAvailability() {
        Date currDate = clock.getCurrentDate();
        Calendar c = Calendar.getInstance(this.timeZone);
        c.setTimeInMillis(currDate.getTime());
        return getAvailability(c.get(Calendar.DAY_OF_WEEK),
                c.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * Creates a String representation of the availability information.
     * @return a String representation
     */
    @Override
    public String toString() {
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder result =
                new StringBuilder("--------------Availability---------------\n");
        for (int d = Calendar.SUNDAY; d <= Calendar.SATURDAY; d++) {
            result.append(days[d - 1]).append(" ");
            for (int h = 0; h < 24; h++) {
                result.append(avail[(d - 1) * 24 + h]).append(" ");
            }
            result.append("\n");
        }
        result.append("-----------------------------------------");
        return result.toString();
    }
}
