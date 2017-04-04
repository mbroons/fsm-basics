package com.alu.oamp.fsm;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An emulation of a single threaded scala actor.
 *
 * The actor embeds a single threaded executor service. The client applications
 * sends the messages to be processed by the actor using the send method. The
 * message sent is processed by the background thread by the onMessage method.
 *
 * A concrete implementation of the actor has to subclass the onMessage method
 * to implement the message processing.
 *
 * The actor is stopped using the shutdown method.
 *
 * @param <T>
 *            the message sent to the actor
 *
 * @author tvillard
 *
 */
public abstract class Actor<T> {
	private static final int DEFAULT_CAPACITY = 1000;
	private static final int DEFAULT_SHUTDOWN_DELAY = 5;
	// No static logger in libraries
	private final Logger logger = LoggerFactory.getLogger(Actor.class);

	private final ExecutorService exec;
	private final int shutdownDelay;

	/**
	 * Creates a new actor.
	 *
	 * @param capacity
	 *            the actor capacity.
	 * @param shutdownDelay
	 *            the shutdown delay.
	 * @param clazz
	 *            the actor concrete class (which gives the actor thread name).
	 *
	 *            When messages are sent beyond the actor capacity, the message
	 *            are discarded.
	 */
	public Actor(int capacity, Class<?> clazz, int shutdownDelay) {
		this(capacity, clazz.getSimpleName(), shutdownDelay);
	}
	
	/**
	 * Creates a new actor.
	 * 
	 * @param capacity
	 *            the actor capacity.
	 * @param clazz
	 *            the actor concrete class (which gives the actor thread name).
	 */
	public Actor(int capacity, Class<?> clazz) {
		this(capacity, clazz, DEFAULT_SHUTDOWN_DELAY);
	}

	/**
	 * Creates a new actor.
	 *
	 * @param clazz
	 *            the actor concrete class (which gives the actor thread name).
	 */
	public Actor(Class<?> clazz) {
		this(DEFAULT_CAPACITY, clazz, DEFAULT_SHUTDOWN_DELAY);
	}

	/**
	 * Creates a new actor.
	 *
	 * @param capacity
	 *            the actor capacity.
	 * @param shutdownDelay
	 *            the actor shutdown delay.
	 * @param threadName
	 *            the actor thread name.
	 *
	 *            When messages are sent beyond the actor capacity, the message
	 *            are discarded.
	 */
	public Actor(int capacity, String threadName, int shutdownDelay) {
		if (capacity == 0) {
			throw new IllegalArgumentException("capacity can't be 0");
		}
		
		if (shutdownDelay == 0) {
			throw new IllegalArgumentException("shutdown delay can't be 0");
		}
		this.shutdownDelay = shutdownDelay;
		exec = newExecutor(capacity, threadName);
	}

	/**
	 * Creates a new actor.
	 *
	 * @param threadName
	 *            the actor thread name
	 */
	public Actor(String threadName) {
		this(DEFAULT_CAPACITY, threadName, DEFAULT_SHUTDOWN_DELAY);
	}

	/**
	 * shutdown the actor.
	 */
	public void shutdown() {

		try {
			exec.shutdown();
			if (!exec.awaitTermination(shutdownDelay, TimeUnit.SECONDS)) {
				exec.shutdownNow();
				if (!exec.awaitTermination(shutdownDelay, TimeUnit.SECONDS)) {
					logger.error("Actor failed to shutdown");
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Returns true when the actor has shutdown.
	 *
	 * @return true when the actor has shutdown
	 */
	public boolean isShutdown() {

		return exec.isTerminated();
	}

	/**
	 * Sends a message to the actor.
	 *
	 * @param message
	 *            the message sent
	 */
	public void send(T message) {

		if (exec.isShutdown()) {
			throw new IllegalStateException("Actor is shutdown.");
		}

		exec.execute(new MessageProcessor(message));
	}

	/**
	 * A message processor.
	 *
	 * @author tvillard
	 *
	 */
	private class MessageProcessor implements Runnable {

		private final T message;

		public MessageProcessor(T message) {
			this.message = message;
		}

		@Override
		public void run() {
			onMessage(message);
		}

		public T getMessage() {
			return message;
		}
	}

	/**
	 * processes the incoming messages.
	 *
	 * Actor concrete implementations have to implement this method
	 *
	 * @param message
	 *            the received message
	 */
	protected abstract void onMessage(T message);

	private ExecutorService newExecutor(int capacity, final String threadName) {

		ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, threadName);
				thread.setUncaughtExceptionHandler(new UELogger());
				return thread;
			}
		};
		BlockingQueue<Runnable> workQueue =
				new ArrayBlockingQueue<>(capacity);
		return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, workQueue,
			threadFactory, new RELogger());
	}

	/**
	 * The actor unexpected exception handler.
	 *
	 * @author tvillard
	 *
	 */
	private class UELogger implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable exception) {
			logger.error("Task failed with exception", exception);
		}
	}

	/**
	 * The actor rejected execution handler.
	 *
	 * @author tvillard
	 *
	 */
	private class RELogger implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable runnable,
				ThreadPoolExecutor pool) {

			@SuppressWarnings("unchecked")
			MessageProcessor processor = (MessageProcessor) runnable;
			logger.error("Task is rejected, actor queue may be full");
			logger.error("Rejected message: {}", processor.getMessage());
		}
	}
}
