package com.alu.oamp.fsm;

/**
 * A timeout listener.
 *
 */
public interface TimeoutListener {

    /**
     * Invoked when the state has timed out.
     */
	void onTimeout();

    /**
     * Invoked when the heart beat failed
     */
    void onHeartBeatError();
}