package me.marcosassuncao.servsim.profile;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SingleProfileTest {
	SingleProfile pool;
	int numRes = 100;
	int halfRes = 50;
	long slot = 50;
	
	@Before
	public void configProfile() {
		pool = new SingleProfile(numRes);
	}

	@Test
	public void test() {
		long EPOCH = 0L;
		
		ProfileEntry e = pool.checkAvailability(10, 0L, numRes);
		assertEquals(e.getNumResources(), numRes);
		e = pool.checkAvailability(EPOCH);
		
		assertEquals(e.getNumResources(), numRes);
		assertEquals(e.getTime(), EPOCH);
		
		// allocate half of resources over one slot
		pool.allocateResourceRanges(new RangeList(0, halfRes - 1), 0L, slot);
		e = pool.checkAvailability(EPOCH);
		assertEquals(e.getNumResources(), halfRes);
		assertEquals(e.getTime(), 0L);

		// allocate another half of resources over one slot
		pool.allocateResourceRanges(new RangeList(halfRes, numRes - 1), 0L, slot);
		e = pool.checkAvailability(EPOCH);
		assertEquals(e.getNumResources(), 0);
		
		// check half of resources will be available
		e = pool.findStartTime(halfRes, EPOCH, slot);
		assertEquals(e.getNumResources(), numRes);
		assertEquals(e.getTime(), slot);
				
		// allocate resources in the future
		pool.allocateResourceRanges(new RangeList(0, numRes - 1), slot + 10, slot + 20);
		e = pool.checkAvailability(numRes, slot, slot);
		assertNull(e);
		
		// look for a slot of 10 seconds
		e = pool.findStartTime(numRes, EPOCH, 10);
		assertEquals(e.getTime(), slot);
		
		// look for a slot greater than 10 seconds
		e = pool.findStartTime(numRes, EPOCH, slot);
		assertEquals(e.getTime(), slot + 20);
	}
}
