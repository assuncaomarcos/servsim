package me.marcosassuncao.servsim.job;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class DefaultWorkUnitTest {
	DefaultWorkUnit unit;
	int duration = 100;
	
	@Before
	public void createUnit() {
		unit = new DefaultWorkUnit(duration);
	}
	
	@Test
	public void testSubmission() {
		assertEquals(unit.getStatus(), WorkUnit.Status.UNKNOWN);
		unit.setSubmitTime(0);
		assertEquals(unit.getStatus(), WorkUnit.Status.ENQUEUED);
	}
	
	@Test
	public void testStates() {
		long EPOCH = 0L;
		unit.setSubmitTime(EPOCH);
		assertTrue(unit.setStatus(WorkUnit.Status.WAITING, EPOCH));
		assertTrue(unit.setStatus(WorkUnit.Status.IN_EXECUTION, EPOCH));
		assertEquals(unit.getStartTime(), EPOCH);
		assertFalse(unit.setStatus(WorkUnit.Status.ENQUEUED, EPOCH));
		assertTrue(unit.setStatus(WorkUnit.Status.COMPLETE, EPOCH));
		assertEquals(unit.getFinishTime(), EPOCH);
	}
}
