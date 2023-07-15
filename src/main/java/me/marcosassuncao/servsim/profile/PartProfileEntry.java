package me.marcosassuncao.servsim.profile;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * The {@link PartProfileEntry} class represents an entry in the availability
 * profile. It contains the list of ranges of PEs available at a particular
 * time. This time may represent either the start time or completion of
 * a job or advance reservation. It differs from {@link SingleProfileEntry} by
 * having information about multiple resource partitions.
 *
 * @author  Marcos Dias de Assuncao
 *
 * @see Range
 * @see RangeList
 */

public class PartProfileEntry extends ProfileEntry {
    /** The PE ranges in the resource partitions. */
    private final RangeList[] rangesParts;

    /**
     * Creates a new instance of {@link PartProfileEntry}.
     * @param time the time associated with this entry
     * @param numPart the number of partitions
     */
    public PartProfileEntry(final long time, final int numPart) {
        super(time);
        rangesParts = new RangeList[numPart];
    }

    /**
     * Returns the list of ranges available for a given partition at this entry.
     * @param partId the id of the partition.
     * @return the list of ranges available or <code>null</code> if either
     * the partition does not have ranges set.
     * @throws IndexOutOfBoundsException if the partition id is
     * out of the bounds.
     */
    public RangeList getAvailRanges(final int partId) {
        checkElementIndex(partId, rangesParts.length,
                "Partition " + partId + " does not exist");
        return rangesParts[partId];
    }

    /**
     * Returns the overall list of ranges available at this entry in all
     * partitions.
     * @return the list of ranges available or <code>null</code> if no ranges
     * have been set previously.
     */
    public RangeList getAvailRanges() {
        return new PrivResourceRangeList();
    }

    /**
     * Sets the ranges of PEs available at this entry.
     * @param partId the id of the partition.
     * @param availRanges the list of ranges of PEs available
     * @throws IndexOutOfBoundsException if the partition id is
     * out of the bounds.
     */
    public void setAvailRanges(final int partId,
                               final RangeList availRanges) {
        checkElementIndex(partId, rangesParts.length,
                "Partition " + partId + " does not exist");
        rangesParts[partId] = availRanges;
    }

    /**
     * Adds the ranges provided to the list of ranges available.
     * @param partId the id of the partition.
     * @param list the list to be added
     * @throws IndexOutOfBoundsException if the partition id is
     * out of the bounds.
     */
    public void addRanges(final int partId, final RangeList list) {
        checkElementIndex(partId, rangesParts.length,
                "Partition " + partId + " does not exist");
        if (rangesParts[partId] == null) {
            rangesParts[partId] = new RangeList();
        }
        rangesParts[partId].addAll(list);
    }

    /**
     * Gets the number of PEs associated with this entry.
     * @return the number of PEs
     */
    public int getNumResources() {
        if (rangesParts == null) {
            return 0;
        } else {
            int numPE = 0;

            for (RangeList list: rangesParts) {
                if (list != null) {
                    numPE += list.getNumItems();
                }
            }

            return numPE;
        }
    }

    /**
     * Gets the number of PEs at a partition associated with this entry.
     * @param partId the id of the partition.
     * @return the number of PEs
     */
    public int getNumResources(final int partId) {
        checkElementIndex(partId, rangesParts.length,
                "Partition " + partId + " does not exist");
        return rangesParts[partId] == null
                ? 0
                : rangesParts[partId].getNumItems();
    }

    /**
     * Creates a string representation of this entry.
     * @return a representation of this entry
     */
    public String toString() {
        StringBuilder rangeStr = new StringBuilder();
        int numPE = 0;
        for (int i = 0; i < rangesParts.length; i++) {
            RangeList rg = rangesParts[i];
            if (rg != null) {
                numPE += rg.getNumItems();
                rangeStr.append("; queue ").append(i).append("=").append(rg);
            }
        }
        return String.format(
                "ProfileEntry={time=%s; work_units=%s; numPE=%s %s}",
                getTime(), getNumJobs(), numPE, rangeStr);
    }

    /**
     * Returns a clone of this entry. The ranges are cloned, but the time
     * and the number of requests relying on this entry are not.
     * @param time the time for the new entry
     * @return the new entry with the number of requests set to default.
     */
    @Override
    public PartProfileEntry clone(final long time) {
        PartProfileEntry clone = new PartProfileEntry(time, rangesParts.length);
        for (int i = 0; i < rangesParts.length; i++) {
            clone.rangesParts[i] = rangesParts[i] == null
                    ? null
                    : rangesParts[i].clone();
        }
        return clone;
    }


    /**
     * Transfers PEs from partitions to one selected partition.
     * @param partId the partition receiving the ranges
     * @param list the list of ranges
     */
    public void transferPEs(final int partId, final RangeList list) {
        if (partId < 0 || partId >= rangesParts.length) {
            throw new IndexOutOfBoundsException("Partition "
                    + partId + " does not exist");
        }

        for (RangeList range : rangesParts) {
            if (range != null) {
                range.remove(list);
            }
        }

        if (rangesParts[partId] == null) {
            rangesParts[partId] = new RangeList();
        }
        rangesParts[partId].addAll(list.clone());
        rangesParts[partId].mergeRanges();
    }

    // ------------------ PRIVATE METHODS AND CLASSES ---------------------

    /**
     * This class is a modified version of the PERangeList to consider the
     * array of lists of ranges maintained by this entry. There should be a
     * more elegant way to handle the multiple partitions, but this will suffice
     * for the moment.
     *
     * @author Marcos Dias de Assuncao
     *
     * @see RangeList
     */
    private final class PrivResourceRangeList extends RangeList {

        private PrivResourceRangeList() {
            for (RangeList list : PartProfileEntry.this.rangesParts) {
                super.addAll(list.clone());
            }
        }

        /**
         * The method below makes sure that the ranges deleted, are in
         * fact deleted from the ranges of all partitions.
         */
        @Override
        public void remove(final RangeList list) {
            for (RangeList rgs : PartProfileEntry.this.rangesParts) {
                if (rgs != null) {
                    rgs.remove(list);
                }
            }

            // updates the general ranges
            super.remove(list);
        }

        /**
         * The user cannot insert ranges in the list. There should be a better
         * way to ensure that this object will not be modified by including.
         * additional ranges.
         * @return <code>false</code>
         */
        @Override
        public boolean add(final Range rg) {
            throw new UnsupportedOperationException();
        }

        /**
         * The user cannot insert ranges in the list. There should be a better
         * way to ensure that this object will not be modified by including
         * additional ranges.
         * @return <code>false</code>
         */
        @Override
        public boolean addAll(final RangeList rl) {
            throw new UnsupportedOperationException();
        }
    }
}
