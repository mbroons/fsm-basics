package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * An heartbeat able state implements an heart beat mechanism.
 * </p>
 * <p>
 * The client application provides a function that will be invoked at regular intervals.
 * The heartbeat functions gets executed as any internal transitions.
 * It is possible to trigger events from the heartbeat functions, allowing mechanism such as
 * exit the state when a periodically checked conditions applies.
 * </p>
 */
public class HeartbeatAbleState extends AbstractTimedState {


    private final Heartbeat heartbeat;

    /**
     * Creates a new state with heart beat.
     *
     * @param innerState the inner state.
     * @param heartbeat  the heart beat specification
     */
    HeartbeatAbleState(State innerState, Heartbeat heartbeat) {
        super(innerState);
        this.heartbeat = heartbeat;
        provider = () -> new Timer("Heartbeat on " + state.toString());
    }

    @Override
    public void onEntry() {

        state.onEntry();
        timer = provider.get();
        TimerTask runHeartBeat = new TimerTask() {
            public void run() {
                listener.onHeartBeat();

            }
        };
        timer.schedule(runHeartBeat, heartbeat.getPeriod(), heartbeat.getPeriod());
    }

    @Override
    public Set<Transition> getInternal(Set<State> states) {

        Set<Transition> transitions = getInnerStateTransitions(states);

        // add heart beat transition
        transitions.add(Transition.newTransition(states).from(getId())
                .event(SimpleStateMachine.InternalEvent.HEARTBEAT).action(heartbeat.getAction()).build());
        return transitions;
    }
}