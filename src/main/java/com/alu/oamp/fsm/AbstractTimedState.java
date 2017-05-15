package com.alu.oamp.fsm;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.function.Supplier;

/**
 * A abstract timed state.
 */

public abstract class AbstractTimedState implements TimedState {

    protected final State state;
    protected Supplier<Timer> provider;
    protected TimedStateListener listener;
    protected Timer timer;

    /**
     * Creates a new abstract active state.
     *
     * @param innerState the inner state.
     */
    AbstractTimedState(State innerState) {
        this.state = innerState;
    }

    @Override
    public StateId getId() {
        return state.getId();
    }

    @Override
    public void onExit() {

        if (timer != null) {
            timer.cancel();
        }
        state.onExit();
    }

    protected Set<Transition> getInnerStateTransitions(Set<State> states) {

        // Get inner state transitions
        Set<Transition> transitions = new HashSet<>();
        if (state instanceof TimedState) {
            transitions = ((TimedState) state).getInternal(states);
        }
        return transitions;
    }

    @Override
    public void shutdown() {
        if (state instanceof TimedState) {
            ((TimedState) state).shutdown();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void setActiveStateListener(TimedStateListener listener) {
        if (state instanceof TimedState) {
            ((TimedState) state).setActiveStateListener(listener);
        }
        this.listener = listener;
    }

    public void setProvider(Supplier<Timer> provider) {
        this.provider = provider;
        if (state instanceof TimedState) {
            ((TimedState) state).setProvider(provider);
        }
    }

    @Override
    public String toString() {
        return state.toString();
    }
}