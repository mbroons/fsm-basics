package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A timeout able state times out if the state is active for more than the timeout duration.
 */
public class TimeoutAbleState extends AbstractTimedState {

    private final Timeout timeout;

    /**
     * Creates a new state with timeout.
     *
     * @param innerState     the inner state.
     * @param timeout        the timeout specification
     */
    TimeoutAbleState(State innerState, Timeout timeout) {
        super(innerState);
        this.timeout = timeout;
        provider = () -> new Timer("Timer " + state.toString());
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
        timer.schedule(task, timeout.getTimeout());
    }


    @Override
    public Set<Transition> getInternal(Set<State> states) {

        Set<Transition> transitions = getInnerStateTransitions(states);

        // add exit monitoring transition
        transitions.add(Transition.newTransition(states).from(getId())
                .event(SimpleStateMachine.InternalEvent.TIMEOUT)
                .to(timeout.getTargetStateId())
                .action(timeout.getAction()).build());
        return transitions;
    }
}