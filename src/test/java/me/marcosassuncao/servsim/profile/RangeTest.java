package me.marcosassuncao.servsim.profile;

import static org.junit.Assert.*;
import me.marcosassuncao.servsim.profile.Range;
import me.marcosassuncao.servsim.profile.RangeList;

import org.junit.Before;
import org.junit.Test;

public class RangeTest {
	Range rangeFull;
	Range rangeStart;
	Range rangeMiddle;
	Range rangeEnd;
	
	@Before
	public void createRanges() {
		rangeFull = new Range(0, 99);
		rangeStart = new Range(0, 9);
		rangeEnd = new Range(90, 99);
		rangeMiddle = new Range(40, 59);
	}

	@Test
	public void test() {
		assertTrue(rangeFull.getNumItems() == 100);
		assertTrue(rangeFull.intersect(rangeMiddle));
		assertFalse(rangeStart.intersect(rangeEnd));
		
		// test deletion
		RangeList newRanges = rangeFull.difference(rangeStart);
		assertTrue(newRanges.getNumItems() == 90);
		assertEquals(newRanges.toString(), "{[10..99]}");

		// test intersection
		Range intersect = rangeFull.intersection(rangeMiddle);
		assertTrue(intersect.getNumItems() == rangeMiddle.getNumItems());
		assertEquals(intersect.toString(), "[40..59]");
	}
}
