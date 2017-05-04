package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.SECONDS;


public class EventLoopTest {
	
	@Test
	public void test_event_loop_life_cycle() {
		
		StringEventLoop worker = new StringEventLoop();
		worker.shutdown();
		Assert.assertTrue(worker.isShutdown());
	}
	
	@Test
	public void test_send_message() {
		
		StringEventLoop worker = new StringEventLoop();
		worker.send("Msg #1");
		worker.shutdown();
		Assert.assertTrue(worker.isShutdown());
	}
	
	@Test
	public void test_event_loop_worker_is_broken() {
		
		BrokenEventLoop bel = new BrokenEventLoop();
		bel.send("Msg #1");
		bel.send("Msg #2");
		bel.shutdown();
		Assert.assertTrue(bel.isShutdown());
	}
	
	@Test
	public void test_message_is_rejected_when_queue_is_full() {
		
		SleepingEventLoop sel = new SleepingEventLoop(2);
		sel.send("Msg #1");
		sel.send("Msg #2");
		sel.send("Msg #3");
		sel.send("Msg #4");
		sel.shutdown();
		Assert.assertTrue(sel.isShutdown());
	}
	
	@Test(expectedExceptions=IllegalArgumentException.class)
	public void test_exception_is_raised_on_invalid_capacity() {
		
		new SleepingEventLoop(0);
	}
	
	@Test(expectedExceptions=IllegalStateException.class)
	public void test_exception_is_raised_when_sending_message_after_shutdown() {
		
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
