package me.marcosassuncao.servsim.profile;

import com.google.common.collect.ComparisonChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class represents a list of {@link Range}'s. This is used to represent
 * the ranges of resources used by a request and by the allocation policies
 * to create a profile of ranges available at particular simulation times.
 *
 * @author Marcos Dias de Assuncao
 * @see Range
 */

public class RangeList implements Cloneable,
        Iterable<Range>, Comparable<RangeList> {

    /** Stores the number of PEs, to avoid iterating lists frequently. */
    private int numItems = 0;

    /** The ranges of resources in this list. */
    private final ArrayList<Range> ranges = new ArrayList<>();

    /** <code>true</code> if the range is sorted. */
    private boolean sorted = true;

    /** <code>true</code> if the list is not fragmented. */
    private boolean merged = true;

    /** Flag, <code>true</code> if the range has been iterated. */
    private boolean iterated = false;

    /**
     * Default constructor.
     */
    public RangeList() { }

    /**
     * Creates a new {@link RangeList} object.
     * @param from the initial item for this range.
     * @param to the final item for this range.
     */
    public RangeList(final int from, final int to) {
        numItems = to - from + 1;
        ranges.add(new Range(from, to));
    }

    /**
     * Creates a new {@link RangeList} object.
     * @param range a resource range to add to this list
     */
    public RangeList(final Range range) {
        this(range.getBegin(), range.getEnd());
    }

    /**
     * Creates a new {@link RangeList} object.
     * @param str a string containing the ranges of processing
     * elements in a serialised manner.
     */
    public RangeList(final String str) {
        if (!str.matches("(\\{)((\\[)(\\d+)..(\\d+)(\\])[,]?){1,}(\\})?")) {
            throw new IllegalArgumentException(
                    "Invalid list of resource ranges.");
        }

        String subStr = str.substring(str.indexOf('{') + 1,
                str.lastIndexOf('}'));
        String[] parts = subStr.split(",");

        Arrays.stream(parts).forEach(part -> {
            String start = part.substring(part.indexOf('[') + 1,
                    part.indexOf('.'));
            String end = part.substring(part.lastIndexOf('.') + 1,
                    part.indexOf(']'));
            Range rg = new Range(Integer.parseInt(start),
                    Integer.parseInt(end));
            ranges.add(rg);
        });

        sorted = false;
        merged = false;
        iterated = true;
    }

    /**
     * Returns the number of resources in this list.
     * @return the number of resources
     */
    public int getNumItems() {
        if (iterated) {
            numItems = 0;
            ranges.forEach(r -> numItems += r.getNumItems());
            iterated = false;
        }
        return numItems;
    }

    /**
     * Merges resource ranges: e.g.
     * {@literal [3-5],[5-8],[10-20] >= [3-8],[10-20]}.
     */
    public void mergeRanges() {
        if (!merged && ranges.size() > 1) {
            sortRanges();
            int i = 0;
            do {
                Range cr = ranges.get(i);
                Range next = ranges.get(i + 1);
                if ((next.getBegin() - cr.getEnd()) == 1) {
                    cr.setEnd(next.getEnd());
                    ranges.remove(i + 1);
                    continue;
                }
                i++;
            } while (i < ranges.size() - 1);
            merged = true;
        }
    }

    /**
     * Returns a clone of this list of ranges.
     * @return the cloned list
     */
    public RangeList clone() {
        sortRanges();
        RangeList clone = new RangeList();
        ranges.forEach(r -> clone.add(r.clone()));
        clone.sorted = true;
        clone.merged = merged;
        return clone;
    }

    /**
     * Adds a new resource range to this list.
     * @param range the range to be added to the list
     * @return <code>true</code> if the range has been added;
     * <code>false</code> otherwise.
     */
    public boolean add(final Range range) {
        boolean success = ranges.add(range);
        if (success) {
            numItems += range.getNumItems();
        }

        sorted = false;
        merged = false;
        return success;
    }

    /**
     * Adds a list of resource ranges to the list.
     * @param l the list of ranges to be added to the list
     * @return <code>true</code> if the ranges have been added;
     * <code>false</code> otherwise.
     */
    public boolean addAll(final RangeList l) {
        this.remove(l);
        this.numItems += l.numItems;
        sorted = false;
        merged = false;
        return this.ranges.addAll(l.ranges);
    }

    /**
     * Removes all ranges from this list of ranges.
     */
    public void clear() {
        ranges.clear();
        sorted = true;
        merged = true;
        numItems = 0;
    }

    /**
     * Sorts the ranges in this list of ranges.
     */
    public void sortRanges() {
        if (!sorted) {
            if (numItems > 0 && ranges.size() > 1) {
                // Avoid Java's sorting, which dumps the list
                // into an array before sorting
                quickSort(ranges, 0, ranges.size() - 1);
            }
            sorted = true;
        }
    }

    /**
     * Returns the number of item ranges in this list.
     * @return the number of item ranges.
     */
    public int size() {
        return ranges.size();
    }

    /**
     * Returns the smallest item number in this list.
     * @return the smallest item number of <code>-1</code> if not found.
     */
    public int getLowestItem() {
        sortRanges();
        return ranges.size() > 0 ? ranges.get(0).getBegin() : -1;
    }

    /**
     * Returns the greatest item number in this list.
     * @return the greatest item number of <code>-1</code> if not found.
     */
    public int getHighestItem() {
        sortRanges();
        return ranges.size() > 0 ? ranges.get(ranges.size() - 1).getEnd() : -1;
    }

    /**
     * Creates an String representation of this list.
     * @return the string representation
     */
    public String toString() {
        if (ranges.size() == 0) {
            return "{[]}";
        }

        sortRanges();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        int index = -1;
        int last = ranges.size() - 1;

        for (Range range : ranges) {
            index++;
            stringBuilder.append(range);
            if (index < last) {
                stringBuilder.append(",");
            }
        }

        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    /**
     * Returns an iterator for this list.
     * @return an iterator for the ranges in this list.
     */
    public Iterator<Range> iterator() {
        return new PrivateIterator();
    }

    /**
     * Internal Iterator.
     */
    private class PrivateIterator implements Iterator<Range> {

        /** Private range iterator. */
        private final Iterator<Range> it = ranges.iterator();

        /** Next range to be returned. */
        private Range next = null;

        /** Last range returned. */
        private Range lastReturned = null;

        PrivateIterator() {
            if (it.hasNext()) {
                next = it.next();
            }
        }

        @Override
        public boolean hasNext() {
            return (next != null);
        }

        @Override
        public Range next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            iterated = true;
            sorted = false;
            merged = false;
            lastReturned = next;
            next = it.hasNext() ? it.next() : null;
            return lastReturned;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            lastReturned = null;
            it.remove();
        }
    }

    /**
     * Identifies the intersections between lists of ranges.
     * @param listb the second list
     * @return a list containing the intersection between the lists
     */
    public RangeList intersection(final RangeList listb) {
        RangeList rIts = new RangeList();
        if (getNumItems() == 0 || listb.getNumItems() == 0) {
            return rIts;
        }

        sortRanges();
        listb.sortRanges();

        for (Range rq : this.ranges) {
            for (Range ru : listb.ranges) {
                // rq's end is already smaller than the
                // start of the following ranges
                if (rq.getEnd() < ru.getBegin()) {
                    break;
                }

                // No intersection has started yet because ru's end is still
                // smaller than the start of rq, which means ru is still smaller
                if (ru.getEnd() < rq.getBegin()) {
                    continue;
                }

                rIts.add(rq.intersection(ru));
            }
        }

        return rIts;
    }

    /**
     * Removes the ranges provided from this list.
     * @param list the ranges to be removed from this list.
     */
    public void remove(final RangeList list) {
        mergeRanges();
        list.mergeRanges();

        int i = 0;
        RangeList diffRange;
        while (i < ranges.size()) {
            Range rq = ranges.get(i);

            for (Range ru : list.ranges) {
                // if the end of the range of this queue is smaller
                // than the start of the range ru in list, then it means
                // that all following ranges in list are beyond rq
                if (rq.getEnd() < ru.getBegin()) {
                    break;
                }

                // if the end of ru in the list is smaller than the start
                // of rq, then it means that ru is below rq, so just continue
                if (ru.getEnd() < rq.getBegin()) {
                    continue;
                }

                diffRange = rq.difference(ru);
                if (diffRange == null) {
                    ranges.remove(i);
                    numItems -= rq.getNumItems();
                    i--;
                    break;
                } else {
                    numItems -= (rq.getNumItems() - diffRange.getNumItems());
                    Range fr = diffRange.ranges.get(0);
                    rq.setBegin(fr.getBegin());
                    rq.setEnd(fr.getEnd());

                    if (diffRange.size() > 1) {
                        ranges.add(i + 1, diffRange.ranges.get(1));
                        i--;
                        break;
                    }
                }
            }
            i++;
        }

        sorted = false;
        merged = false;
        mergeRanges();
    }

    /**
     * Checks if the current range list is equals to the provided range.
     * @param other the range to compare this range against
     * @return <code>true</code> if they are equal,
     * <code>false</code> otherwise.
     */
    public boolean equals(final RangeList other) {
        if (this == other) {
            return true;
        } else if (getNumItems() != other.getNumItems()) {
            return false;
        } else {
            RangeList list = intersection(other);
            return list.getNumItems() == getNumItems()
                    && list.getNumItems() == other.getNumItems();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RangeList o) {
        return ComparisonChain.start()
                .compare(this, o)
                .compare(getLowestItem(), o.getLowestItem())
                .compare(getHighestItem(), o.getHighestItem())
                .compare(getNumItems(), o.getNumItems()).result();
    }

    /**
     * Selects a range to be used by a request.
     * @param reqRes the number of resources required.
     * @return the range to be allocated or <code>null</code> if no
     * range suitable is found.
     */
    public RangeList selectResources(final int reqRes) {
        if (getNumItems() < reqRes) {
            return null;
        }

        mergeRanges();
        RangeList selected = new RangeList();

        int currRes = reqRes;
        for (Range range : ranges) {
            if (range.getNumItems() >= currRes) {
                int begin = range.getBegin();
                int end = begin + currRes - 1;
                selected.add(new Range(begin, end));
                break;
            } else {
                selected.add(range.clone());
                currRes -= range.getNumItems();
            }
        }

        selected.sorted = true;
        return selected;
    }

    // The methods below were copied from:
    // http://www.mycsresource.net/articles/programming/sorting_algos/quicksort
    private static void quickSort(final ArrayList<Range> array,
                                  final int start,
                                  final int end) {
        int i = start;  // index of left-to-right scan
        int k = end;    // index of right-to-left scan

        if (end - start >= 1) {
            Range pivot = array.get(start);
            while (k > i) {
                while (array.get(i).getBegin() <= pivot.getBegin()
                        && i <= end && k > i) {
                    i++;
                }

                while (array.get(k).getBegin() > pivot.getBegin()
                        && k >= start && k >= i) {
                    k--;
                }

                if (k > i) {
                    swap(array, i, k);
                }
            }
            swap(array, start, k);
            quickSort(array, start, k - 1); // quicksort the left partition
            quickSort(array, k + 1, end); 	// quicksort the right partition
        }
    }

    /*
     * Swap indices in the array
     */
    private static void swap(final ArrayList<Range> array,
                             final int index1,
                             final int index2) {
        Range temp = array.get(index1);
        array.set(index1, array.get(index2));
        array.set(index2, temp);
    }
}
