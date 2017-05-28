package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * An heartbeat able state implements an heart beat mechanism.
 * </p>
 * <p>
 * The client application provides an heart beat function that will be regularly invoked.
 * </p>
 */
public class DefaultHeartbeatAbleState extends AbstractTimedState {


    private final DefaultHeartbeat heartbeat;

    /**
     * Creates a new state with heart beat.
     *
     * @param innerState the inner state.
     * @param heartbeat  the heart beat specification
     */
    DefaultHeartbeatAbleState(State innerState, DefaultHeartbeat heartbeat) {
        super(innerState);
        this.heartbeat = heartbeat;
        provider = () -> new Timer("Monitor " + state.toString());
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
        transitions.add(Transition.newBuilder(states).from(getId())
                .event(SimpleStateMachine.InternalEvent.HEARTBEAT).action(heartbeat.getHeartbeatAction()).build());
        return transitions;
    }
}