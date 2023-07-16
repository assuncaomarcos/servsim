package me.marcosassuncao.servsim.server;

import static org.junit.Assert.*;
import me.marcosassuncao.servsim.profile.ProfileEntry;
import me.marcosassuncao.servsim.profile.RangeList;
import me.marcosassuncao.servsim.server.DefaultResourcePool;

import org.junit.Before;
import org.junit.Test;

public class DefaultResourcePoolTest {
	DefaultResourcePool pool;
	int capacity = 10;

	@Before
	public void setUp() throws Exception {
		pool = new DefaultResourcePool(capacity);
	}

	@Test
	public void testAllocation() {
		RangeList range = new RangeList(0,9);
		ProfileEntry e = pool.checkAvailability(10, 0L, 10);
		assertEquals(e.getTime(), 0L);
		assertTrue(e.getAvailRanges().equals(range));
		
		pool.allocateResources(range, 0L, 100L);
		e = pool.checkAvailability(10, 0L, 10);
		assertEquals(e, null);
		e = pool.findStartTime(10, 10, 10);
		assertEquals(e.getTime(), 100L);
		pool.releaseResources(0L, 100L, range);
		e = pool.checkAvailability(10, 0L, 10);
		assertEquals(e.getTime(), 0L);
		assertTrue(e.getAvailRanges().equals(range));
	}
}
