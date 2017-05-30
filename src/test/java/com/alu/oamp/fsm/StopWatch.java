package com.alu.oamp.fsm;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.alu.oamp.fsm.States.newState;
import static com.alu.oamp.fsm.Transition.newTransition;
import static com.alu.oamp.fsm.Heartbeat.newHeartbeat;

/**
 * This is a simple stopwatch.
 * <p>
 * States are [STOPPED; STARTED]
 * and 2 commands for the watch: start and stop and reset
 * </p>
 * <p>On START, the stopwatch starts</p>
 * <p>On STOP, the stopwatch stops</p>
 * <p>On RESET, the stopwatch resets</p>
 * Several scenario are tested.
 */
public class StopWatch {

    private SimpleStateMachine fsm;

    enum Cmd implements EventId {
        START,
        STOP
    }

    enum State implements StateId {

        STOPPED,
        STARTED
    }

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    static StopWatch newStopWatch(SimpleStateListener listener) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.init(listener);
        return stopWatch;
    }

    private SimpleStateMachine init(SimpleStateListener listener) {

        Set<com.alu.oamp.fsm.State> states = new HashSet<>();

        com.alu.oamp.fsm.State state = newState(State.STARTED)
                .heartbeat(newHeartbeat()
                        .period(1000)
                        .action(() -> System.out.println("Now is "+ new Date()))
                        .build())
                .build();
        states.add(state);

        state = newState(State.STOPPED).build();
        com.alu.oamp.fsm.State initial = state;
        states.add(state);

        Set<Transition> transitions = new HashSet<>();

        // Transition to open the door
        Transition transition =
                newTransition(states).from(State.STOPPED)
                        .event(Cmd.START).to(State.STARTED).build();

        transitions.add(transition);
        transition =
                newTransition(states).from(State.STARTED)
                        .event(Cmd.STOP).to(State.STOPPED).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "Simple Stop Watch", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }


    public void shutdown() {
        fsm.shutdown();
    }
}
