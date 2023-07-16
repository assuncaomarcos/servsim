package me.marcosassuncao.servsim.profile;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class represents the profile containing the ranges of PEs
 * available at given simulation times. This availability profile does not
 * provide features to manage multiple resource partitions.
 *
 * @author Marcos Dias de Assuncao
 *
 * @see Profile
 * @see SingleProfileEntry
 * @see RangeList
 * @see TimeSlot
 */

public class SingleProfile extends Profile<SingleProfileEntry>
            implements Cloneable, Iterable<SingleProfileEntry> {

    /**
     * Creates an availability profile for a resource providing the number
     * of processors in the resource. This constructor will create an initial
     * {@link SingleProfileEntry} with time <code>0</code> and
     * an initial PE range of [0, numPE-1].
     * @param numPE the number of processors in the resource.
     * @see SingleProfileEntry
     */
    public SingleProfile(final int numPE) {
        RangeList ranges = new RangeList(0, numPE - 1);
        SingleProfileEntry entry = new SingleProfileEntry(0L, ranges);
        add(entry);
    }

    // ------------------- PROTECTED CONSTRUCTORS -----------------------

    /**
     * Protected constructor used by the cloning operations.
     * @param avail the availability information map.
     * @see SingleProfileEntry
     */
    protected SingleProfile(final LinkedTreeMap<Long,
            SingleProfileEntry> avail) {
        this.avail.putAll(avail);
    }

    /**
     * Creates a new Profile object.<br>
     * <b>NOTE:</b> if you use this constructor, you need to insert an initial
     * entry with the number of PEs.
     * @see SingleProfileEntry
     */
    protected SingleProfile() { }

    // --------------------------- PUBLIC METHODS -----------------------

    /**
     * Returns shallow copy of this object.<br>
     * <b>NOTE:</b> this method does not clone the entries.
     * @return the cloned object
     * @see SingleProfile#copy()
     */
    public SingleProfile clone() {
        return new SingleProfile(super.avail);
    }

    /**
     * Returns copy of this object.<br>
     * <b>NOTE:</b> this method clones the entries
     * @return the copy object
     */
    public SingleProfile copy() {
        SingleProfile copy = new SingleProfile();
        avail.forEach((time, entry) -> copy.add(entry.clone(time)));
        return copy;
    }

    /**
     * Includes a time slot in this availability profile. This is useful if
     * your scheduling strategy cancels a job and you want to update the
     * availability profile.
     * @param startTime the start time of the time slot.
     * @param finishTime the finish time of the time slot.
     * @param list the list of ranges of resources in the slot.
     * @return <code>true</code> if the slot was included;
     * <code>false</code> otherwise.
     */
    public boolean addTimeSlot(final long startTime,
                               final long finishTime,
                               final RangeList list) {
        if (finishTime <= startTime) {
            return false;
        }
        Iterator<SingleProfileEntry> it = avail.itValuesFromPrec(startTime);
        SingleProfileEntry last = it.next();
        SingleProfileEntry newAnchor = null;

        if (last.getTime() < startTime) {
            newAnchor = last.clone(startTime);
            last = newAnchor;
        }

        SingleProfileEntry nextEntry;
        while (it.hasNext()) {
            nextEntry = it.next();
            if (nextEntry.getTime() > finishTime) {
                break;
            }

            // Remove duplicate entries. That is, entries whose PE ranges
            // are the same. This minimises the number of entries required
            if (nextEntry.getTime() < finishTime
                    && last.getAvailRanges().equals(
                            nextEntry.getAvailRanges())) {
                it.remove();
            } else {
                last.getAvailRanges().addAll(list.clone());
                last = nextEntry;
            }
        }

        if (last.getTime() < finishTime) {
            add(last.clone(finishTime));
            last.getAvailRanges().addAll(list.clone());
        }

        if (newAnchor != null) {
            add(newAnchor);
        }

        return true;
    }

    /**
     * Returns an iterator in case someone needs to iterate this
     * object. <br>
     * <b>NOTE:</b> Removing objects from this profile via its iterator
     * may make it behave in an unexpected way.
     *
     * @return an iterator for the {@link SingleProfileEntry}
     * objects in this profile.
     */
    public Iterator<SingleProfileEntry> iterator() {
        return new PrivateValueIterator();
    }


    // ------------------- PRIVATE METHODS -----------------------

    /**
     * A delegation based iterator in case someone needs to iterate this
     * object. <br>
     * <b>NOTE:</b> Removing objects from this profile via its iterator
     * may make it behave in an unexpected way.
     *
     * @author Marcos Dias de Assuncao
     */
    private class PrivateValueIterator implements Iterator<SingleProfileEntry> {

        /** Private entry iterator. */
        private final Iterator<SingleProfileEntry> it;

        PrivateValueIterator() {
            it = avail.values().iterator();
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public SingleProfileEntry next() {
            if (!it.hasNext()) {
                throw new NoSuchElementException();
            }
            return it.next();
        }

        public void remove() {
            it.remove();
        }
    }
}
