package com.alu.oamp.fsm;

import java.util.Timer;

/**
 * A timer provider interface
 *
 */
public interface TimerProvider {

	/**
	 * Returns a new timer.
	 * @return a new timer
	 */
	Timer get();
}