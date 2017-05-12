package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A timeout able state times out if the state is active for more than the timeout duration
 */
public class TimeoutAbleState extends AbstractTimedState {

    private final long timeout;
    private final Runnable timeoutAction;
    private final StateId timeoutStateId;

    /**
     * Creates a new state with timeout.
     *
     * @param innerState     the inner state.
     * @param timeout        the transient state timeout
     * @param timeoutAction  the transient state timeout action
     * @param timeoutStateId the target state on timeout
     */
    TimeoutAbleState(State innerState, long timeout, Runnable timeoutAction,
                     StateId timeoutStateId) {
        super(innerState);
        this.timeout = timeout;
        this.timeoutAction = timeoutAction;
        this.timeoutStateId = timeoutStateId;
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
        timer.schedule(task, timeout);
    }


    @Override
    public Set<Transition> getInternal(Set<State> states) {

        Set<Transition> transitions = getInnerStateTransitions(states);

        // add exit monitoring transition
        transitions.add(Transition.newBuilder(states).from(getId())
                .event(SimpleStateMachine.InternalEvent.TIMEOUT)
                .to(timeoutStateId)
                .action(timeoutAction).build());
        return transitions;
    }
}