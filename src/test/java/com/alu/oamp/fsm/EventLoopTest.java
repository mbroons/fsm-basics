package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.SECONDS;


public class EventLoopTest {
	
	@Test
	public void testStartStop() {
		
		StringEventLoop worker = new StringEventLoop();
		worker.shutdown();
		Assert.assertTrue(worker.isShutdown());
	}
	
	@Test
	public void testSendMessage() {
		
		StringEventLoop worker = new StringEventLoop();
		worker.send("Msg #1");
		worker.shutdown();
		Assert.assertTrue(worker.isShutdown());
	}
	
	@Test
	public void testTasksInException() {
		
		BrokenEventLoop bel = new BrokenEventLoop();
		bel.send("Msg #1");
		bel.send("Msg #2");
		bel.shutdown();
		Assert.assertTrue(bel.isShutdown());
	}
	
	@Test
	public void testSleepingTasks() {
		
		SleepingEventLoop sel = new SleepingEventLoop(10);
		sel.send("Msg #1");
		sel.send("Msg #2");
		sel.shutdown();
		Assert.assertTrue(sel.isShutdown());
	}
	
	@Test
	public void testRejectedExecution() {
		
		SleepingEventLoop sel = new SleepingEventLoop(2);
		sel.send("Msg #1");
		sel.send("Msg #2");
		sel.send("Msg #3");
		sel.send("Msg #4");
		sel.shutdown();
		Assert.assertTrue(sel.isShutdown());
	}
	
	@Test(expectedExceptions=IllegalArgumentException.class)
	public void testInvalidCapacity() {
		
		new SleepingEventLoop(0);
	}
	
	@Test(expectedExceptions=IllegalStateException.class)
	public void testProcessAfterShutdown() {
		
		StringEventLoop worker = new StringEventLoop();
		worker.shutdown();
		Assert.assertTrue(worker.isShutdown());
		worker.send("Msg #1");
	}
	
	public static class SleepingEventLoop extends AbstractEventLoop<String> {
		
		public SleepingEventLoop(int capacity) {
			super(capacity, SleepingEventLoop.class);
		}

		@Override
		protected void onMessage(String msg) {
			try {
				SECONDS.sleep(Long.MAX_VALUE);
			} catch (InterruptedException ignored) {
			}
		}
		
	}
	
	public static class BrokenEventLoop extends AbstractEventLoop<String> {
		
		public BrokenEventLoop() {
			super(BrokenEventLoop.class);
		}

		@Override
		protected void onMessage(String msg) {
			
			throw new RuntimeException("I broke folks...");
		}
	}
	
	public static class StringEventLoop extends AbstractEventLoop<String> {
		
		public StringEventLoop() {
			super(StringEventLoop.class);
		}
		
		private static final Logger LOGGER =
			LoggerFactory.getLogger(StringEventLoop.class);

		@Override
		protected void onMessage(String msg) {
			
			LOGGER.info("I process message: {}", msg);
		}
	}
}
