package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.SECONDS;


public class AbstractEventLoopTest {
	
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
		
		ErrorEventLoop eWorker = new ErrorEventLoop();
		eWorker.send("Msg #1");
		eWorker.send("Msg #2");
		eWorker.shutdown();
		Assert.assertTrue(eWorker.isShutdown());
	}
	
	@Test
	public void testSleepingTasks() {
		
		SleepingEventLoop sWorker = new SleepingEventLoop(10);
		sWorker.send("Msg #1");
		sWorker.send("Msg #2");
		sWorker.shutdown();
		Assert.assertTrue(sWorker.isShutdown());
	}
	
	@Test
	public void testRejectedExecution() {
		
		SleepingEventLoop actor = new SleepingEventLoop(2);
		actor.send("Msg #1");
		actor.send("Msg #2");
		actor.send("Msg #3");
		actor.send("Msg #4");
		actor.shutdown();
		Assert.assertTrue(actor.isShutdown());
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
	
	public static class ErrorEventLoop extends AbstractEventLoop<String> {
		
		public ErrorEventLoop() {
			super(ErrorEventLoop.class);
		}

		@Override
		protected void onMessage(String msg) {
			
			throw new RuntimeException("I failed folks...");
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
