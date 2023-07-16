package me.marcosassuncao.servsim.profile;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class represents a closed range of integers. This is used by allocation
 * policies to keep track of processors available at a particular time and
 * the processors allocated to work units. For example, a work unit is
 * using a range of [0..4].
 *
 * @author Marcos Dias de Assuncao
 *
 * @see RangeList
 */

public class Range implements Cloneable, Comparable<Range>, Iterable<Integer> {

    /** Element at the beginning of the range. */
    private int begin;

    /** Element at the end of the range. */
    private int end;

    /**
     * Creates a new {@link Range} object.
     * @param begin the start of the range of PEs
     * @param end the end of the range
     */
    public Range(final int begin, final int end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Returns the beginning of the range.
     * @return the number corresponding to the beginning
     */
    public int getBegin() {
        return begin;
    }

    /**
     * Returns the end of the range of PEs.
     * @return the end of the range
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the number of items in this range.
     * @return the number of items
     */
    public int getNumItems() {
        return (end - begin) + 1;
    }

    /**
     * Returns a clone of this range.
     * @return the cloned range
     */
    public Range clone() {
        return new Range(begin, end);
    }

    /**
     * Compares this range against another range of PEs.
     * @param range the range to compare this range with
     * @return <code>-1</code> if the  beginning of this range is
     * smaller than the other range, <code>0</code> if they are
     * the same and <code>1</code> the beginning of this range is bigger
     */
    public int compareTo(final Range range) {
        return Integer.compare(begin, range.begin);
    }

    /**
     * Creates a string representation of this class.
     * @return the string representation
     */
    public String toString() {
        return "[" + begin + ".." + end + "]";
    }


    // ----------------- PACKAGE LEVEL METHODS -------------------

    /**
     * Returns the common range of this range with another.
     * @param rangeb the second range
     * @return the common range of PEs
     */
    Range intersection(final Range rangeb) {
        if (rangeb == null) {
            return null;
        }

        int s = Math.max(this.begin, rangeb.begin);
        int e = Math.min(this.end, rangeb.getEnd());
        return (s > e) ? null : new Range(s, e);
    }

    /**
     * Returns the list of ranges resulting from subtracting the
     * given range from this range.
     * @param rangeb the range to compare this range against
     * @return the range corresponding to the difference
     */
    RangeList difference(final Range rangeb) {
        RangeList difference = new RangeList();

        int s = Math.min(this.begin, rangeb.begin);
        int e = (this.end < rangeb.begin) ? this.end : rangeb.begin - 1;

        if (s <= e) {
            difference.add(new Range(s, e));
        }

        s = (this.begin <= rangeb.end) ? rangeb.end + 1 : this.begin;
        e = Math.max(this.end, rangeb.end);

        if (s <= e) {
            difference.add(new Range(s, e));
        }

        return difference.size() == 0 ? null : difference;
    }

    /**
     * Checks whether this range intersects with the given range.
     * @param rangeb the range to compare this range against
     * @return <tt>true</tt> if the two ranges have an intersection
     * or <tt>false</tt> otherwise.
     */
    boolean intersect(final Range rangeb) {
        return intersection(rangeb) != null;
    }

    /**
     * Sets the beginning of the range.
     * @param beginning the beginning
     */
    void setBegin(final int beginning) {
        this.begin = beginning;
    }

    /**
     * Sets the end of the PE range.
     * @param end the end of the range
     */
    void setEnd(final int end) {
        this.end = end;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Integer> iterator() {
        return new PrivateIterator();
    }

    class PrivateIterator implements Iterator<Integer> {
        /**
         * Pointer to current item in the iteration.
         */
        private int current = begin;

        @Override
        public boolean hasNext() {
            return current <= end;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current++;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
