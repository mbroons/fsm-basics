package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic state machine state.
 * 
 */
public class BaseState implements State {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseState.class);

	private final Enum<?> stateId;
	private final Runnable onEntry;
	private final Runnable onExit;

	/**
	 * Creates a new state.
	 * 
	 * @param stateId
	 *            the state id
	 * @param onEntry
	 *            the runnable to execute on entering the state
	 * @param onExit
	 *            the runnable to execute on exiting the state
	 */
	BaseState(Enum<?> stateId, Runnable onEntry, Runnable onExit) {
		this.stateId = stateId;
		this.onEntry = onEntry;
		this.onExit = onExit;
	}

	@Override
	public Enum<?> getId() {
		return stateId;
	}

	@Override
	public void onEntry() {
		if (onEntry != null) {
			try {
				onEntry.run();
			} catch (Exception ex) {
				LOGGER.error("Exception on entry " + toString(), ex);
			}
		}
	}

	@Override
	public void onExit() {
		if (onExit != null) {
			try {
				onExit.run();
			} catch (Exception ex) {
				LOGGER.error("Exception on exit " + toString(), ex);
			}
		}
	}

    @Override
	public String toString() {
		return "[" + stateId + "]";
	}
}