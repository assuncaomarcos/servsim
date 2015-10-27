package me.marcosassuncao.servsim;

import static org.junit.Assert.assertEquals;
import me.marcosassuncao.servsim.SimEntity;
import me.marcosassuncao.servsim.SimEvent;
import me.marcosassuncao.servsim.Simulation;

import org.junit.Test;

public class SimPingTest {
	PongEntity pong;
	PingEntity ping;
	long interval = 5;
	int numberPing = 3;
	
	@Test
	public void runSimulation() {
		Simulation sim = new Simulation() {	
			@Override
			public void onConfigure() {
				pong = new PongEntity("Pong");
				ping = new PingEntity("Ping", pong.getId(), interval, numberPing);
				registerEntity(pong);
				registerEntity(ping);
			}
		};
		
		sim.run();
		assertEquals(pong.pingReceived, numberPing);
		assertEquals(ping.pongReceived, numberPing);
	}
		
	// Simple entity that sends a task to another
	class PingEntity extends SimEntity {
		private int dstEntity;
		private long interval;
		private int numberPing;
		private int pongReceived = 0;

		public PingEntity(String name, int dstEntity, 
				long interval, int numberPing) throws IllegalArgumentException {
			super(name);
			this.dstEntity = dstEntity;
			this.interval = interval;
			this.numberPing = numberPing;
		}

		@Override
		public void onStart() { 
			for (int i = 1; i <= numberPing; i++) {
				super.send(dstEntity, interval * i, SimEvent.Type.TASK_ARRIVE, null);
			}
		}

		@Override
		public void process(SimEvent ev) { 
			if (ev.type() == SimEvent.Type.TASK_COMPLETE) {
				pongReceived++;
			}
		}

		@Override
		public void onShutdown() { }
	}
	
	// Simple entity that received a task and returns it to sender
	class PongEntity extends SimEntity {
		private int pingReceived;

		public PongEntity(String name) throws IllegalArgumentException {
			super(name);
		}

		@Override
		public void onStart() { }

		@Override
		public void process(SimEvent ev) { 
			if (ev.type() == SimEvent.Type.TASK_ARRIVE) {
				pingReceived++;
				// send pong
				super.send(ev.source(), SimEvent.SEND_NOW, SimEvent.Type.TASK_COMPLETE, null);
			}
		}
		
		@Override
		public void onShutdown() { }
	}
}