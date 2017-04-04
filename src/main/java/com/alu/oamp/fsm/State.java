package com.alu.oamp.fsm;

/**
 * A state machine state.
 *
 */
public interface State {

	/**
	 * Returns the state id.
	 * @return the state id.
	 */
	Enum<?> getId();

	/**
	 * Invoked when the state is entered.
	 */
	void onEntry();

	/**
	 * Invoked when the state is exited.
	 */
	void onExit();

}