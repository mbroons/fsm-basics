package com.alu.oamp.fsm;

import java.util.Set;

/**
 * An active state.
 *
 * <p>
 * Active states are state with timeout and/or state with monitoring
 * </p>
 * <p>
 * They need to be shutdown (they hold a timer)
 * </p>
 * <p>
 * They have internal transitions
 * </p>
 *
 */
public interface ActiveState extends State {

	/**
	 * Set active state listener
	 *
	 * @param listener
	 *            the active state listener
	 */
	void setActiveStateListener(TimeoutListener listener);

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