package com.alu.oamp.fsm;

/**
 *
 * A state machine transaction action with an input value.
 *
 * @param <T>
 *            the input parameter type
 *
 */
public interface Action<T> {

	/**
	 * Executes the action.
	 *
	 * @param message
	 *            the message used when firing the transaction.
	 */
	void run(T message);

}
