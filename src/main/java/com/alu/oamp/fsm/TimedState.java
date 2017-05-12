package com.alu.oamp.fsm;

import java.util.Set;

/**
 * A timed state.
 *
 * <p>
 * timed states can exit after a specified duration.
 * </p>
 * <p>
 * Timed state must be shutdown before exiting (to shutdown the timer)
 * </p>
 * <p>
 * The creation of a timed state results in the creation of internal transition within the state machine.
 * </p>
 *
 */
public interface TimedState extends State {

	/**
	 * Set active state listener
	 *
	 * @param listener
	 *            the active state listener
	 */
	void setActiveStateListener(TimedStateListener listener);

	/**
	 * Returns the state internal transitions.
	 *
	 *
	 * @param states
	 *            the state set.
	 * @return the state internal transition
	 *
	 *         <p>
	 *         states with timeout and with monitoring have internal
	 *         transitions
	 *         </p>
	 */
	Set<Transition> getInternal(Set<State> states);

	/**
	 * Shutdown the state.
	 *
	 * <p>
	 * active states must be shutdown
	 * </p>
	 */
	void shutdown();

	/**
	 * Sets the timer provider.
	 *
	 * @param provider
	 *            the time provider.
	 *
	 */
	void setProvider(TimerProvider provider);

}