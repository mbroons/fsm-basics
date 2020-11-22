package com.alu.oamp.fsm;

/**
 *
 * A state machine transition action with an input value.
 *
 *
 */
public interface Action {

	/**
	 * Executes the action.
	 *
	 * @param message
	 *            the message used when firing the transaction.
	 */
	// FIXME would it be possible to type the message ?
	void run(Object message);

}
