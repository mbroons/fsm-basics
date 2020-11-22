package com.alu.oamp.fsm;

/**
 * A timeout listener.
 *
 */
public interface TimedStateListener {

    /**
     * Invoked when the state has timed out.
     */
	void onTimeout();

    /**
     * Invoked when the heart beat period has elapsed.
     */
    void onHeartBeat();
}