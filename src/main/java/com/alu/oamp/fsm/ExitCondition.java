package com.alu.oamp.fsm;

/**
 * A state with monitoring exit condition.
 *
 * <p>The exit condition is periodically checked.</p>
 * <p>The state is exited when the exit condition applies.</p>
 */
public interface ExitCondition {

    /**
     * Returns true when the state should exit.
     * @return true when the state should exit.
     */
    boolean apply();
}
