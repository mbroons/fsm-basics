package com.alu.oamp.fsm;

import java.util.HashSet;
import java.util.Set;

/**
 * A lift door with a heart beat state
 * <p>
 * States are [OPENED; CLOSE; OPENED_AND_RINGING]
 * and the following events for the door:
 * OPEN to open the door.
 * PRESENCE to notify the door that someone/something blocks. (coming from a sensor somewhere)
 * ABSENCE to notify the door that nothing blocks the door
 * </p>
 *     <p>On OPEN the door moves to the OPENED state</p>
 *     <p>The OPENED state times out after 500 ms and the door returns to the CLOSED state if no presence is detected</p>
 *     <p>The OPENED state moves to OPENED_AND_RINGING is someone/something is blocking the door for more than 1000 ms/p>
 * Several scenario are tested.
 */
public class LiftDoorWithHeartBeat {

    private SimpleStateMachine fsm;
    private volatile boolean closeable = true;
    private volatile boolean ringing = false;

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    enum Cmd implements EventId {
        OPEN,
        PRESENCE,
        ABSENCE
    }

    enum State implements StateId {

        OPENED,
        OPENED_AND_RINGING,
        CLOSED
    }

    static LiftDoorWithHeartBeat newDoor(DoorStateListener listener) {
        LiftDoorWithHeartBeat liftDoor = new LiftDoorWithHeartBeat();
        liftDoor.initWithHeartBeat(listener);
        return liftDoor;
    }

    private SimpleStateMachine initWithHeartBeat(DoorStateListener listener) {

        Set<com.alu.oamp.fsm.State> states = new HashSet<>();

        com.alu.oamp.fsm.State state =
                States.newBuilder(State.OPENED)
                        .heartBeatPeriod(50)
                        .heartBeatTimeoutTarget(State.CLOSED)
                        .heartBeatWorker(() -> closeable)
                        .timeout(1000)
                        .timeoutTarget(State.OPENED_AND_RINGING)
                        .build();
        states.add(state);

        // Door is opened and ringing, when door closes stop the bell
        state =
                States.newBuilder(State.OPENED_AND_RINGING)
                        .onEntry(() -> ringing = true)
                        .heartBeatPeriod(50)
                        .heartBeatTimeoutTarget(State.CLOSED)
                        .heartBeatWorker(() -> closeable)
                        .onExit(() -> ringing = false)
                        .build();
        states.add(state);

        state = States.newBuilder(State.CLOSED).build();
        com.alu.oamp.fsm.State initial = state;
        states.add(state);

        // Transition to open the door
        Set<Transition> transitions = new HashSet<>();
        Transition transition =
                Transition.newBuilder(states).from(State.CLOSED)
                        .event(Cmd.OPEN).to(State.OPENED).build();
        transitions.add(transition);

        // Transition for presence detection on opened state
        transition =
                Transition.newBuilder(states).from(State.OPENED)
                        .event(Cmd.PRESENCE).action(() -> closeable = false).build();
        transitions.add(transition);
        transition =
                Transition.newBuilder(states).from(State.OPENED)
                        .event(Cmd.ABSENCE).action(() -> closeable = true).build();
        transitions.add(transition);

        // Transition for presence detection on opened and ringing state state
        transition =
                Transition.newBuilder(states).from(State.OPENED_AND_RINGING)
                        .event(Cmd.ABSENCE).action(() -> closeable = true).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "Heart Beat Door", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }


    public void shutdown() {
        fsm.shutdown();
    }

    public boolean isRinging() {
        return ringing;
    }

}
