package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A state with timeout.
 */
public class StateWithTimeout extends AbstractActiveState {

    private final long timeout;
    private final Runnable timeoutAction;
    private final Enum<?> timeoutStateId;

    /**
     * Creates a new state with timeout.
     *
     * @param innerState     the inner state.
     * @param timeout        the transient state timeout
     * @param timeoutAction  the transient state timeout action
     * @param timeoutStateId the target state on timeout
     */
    StateWithTimeout(State innerState, long timeout, Runnable timeoutAction,
                     Enum<?> timeoutStateId) {
        super(innerState);
        this.timeout = timeout;
        this.timeoutAction = timeoutAction;
        this.timeoutStateId = timeoutStateId;
        provider = new TimerProvider() {
            @Override
            public Timer get() {
                return new Timer("Timer " + state.toString());
            }
        };
    }

    @Override
    public void onEntry() {

        state.onEntry();
        timer = provider.get();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                listener.onTimeout();
            }
        };
        timer.schedule(task, timeout);
    }


    @Override
    public Set<Transition> getInternal(Set<State> states) {

        Set<Transition> transitions = getInnerStateTransitions(states);

        // add exit monitoring transition
        transitions.add(Transition.newBuilder(states).from(getId())
                .event(FiniteStateMachine.InternalEvent.TIMEOUT)
                .to(timeoutStateId)
                .action(timeoutAction).build());
        return transitions;
    }
}