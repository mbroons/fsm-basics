package com.alu.oamp.fsm;

/**
 * A listener for state change events
 */
public interface StateMachineListener {

    void onStateEntered(StateId state);

    void onStateExited(StateId state);
}
