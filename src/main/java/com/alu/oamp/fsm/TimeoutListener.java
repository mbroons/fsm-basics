package com.alu.oamp.fsm;

/**
 * An active state listener.
 *
 */
public interface TimeoutListener {

	/**
	 * Invoked when the state has timed out.
	 */
	void onTimeout();

    /**
     * Invoked when the state exit condition is true .
     */
    void onExitMonitoring();
}