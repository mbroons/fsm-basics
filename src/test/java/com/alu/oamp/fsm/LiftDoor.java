package com.alu.oamp.fsm;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * This is a simple state machine simulating a lift door.
 * <p>
 * States are [Opened; Closed; Opened_and_Ringing]
 * and 1 command for the door: Open the door.
 * <p>
 * Several scenario are tested.
 */
public class LiftDoor {

    private SimpleStateMachine fsm;
    private volatile boolean closeable = false;
    private Bell bell = new Bell();

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    enum Cmd implements EventId {
        OPEN
    }

    enum LiftDoorState implements StateId {

        OPENED,
        OPENED_AND_RINGING,
        CLOSED
    }

    static LiftDoor newSimpleLiftDoor(DoorStateListener listener) {
        LiftDoor liftDoor = new LiftDoor();
        liftDoor.initWithTimeOut(listener);
        return liftDoor;
    }

    static LiftDoor newLiftDoorWithHeardBeat(DoorStateListener listener) {
        LiftDoor liftDoor = new LiftDoor();
        liftDoor.initWithHeartBeat(listener);
        return liftDoor;
    }

    private SimpleStateMachine initWithTimeOut(DoorStateListener listener) {

        Set<State> states = new HashSet<>();

        // The door stays opened for 500 ms and closes itself.
        State state = States.newBuilder(LiftDoorState.OPENED)
                .timeout(500)
                .timeoutTarget(LiftDoorState.CLOSED)
                .build();
        states.add(state);

        state = States.newBuilder(LiftDoorState.CLOSED).build();
        State initial = state;
        states.add(state);

        Set<Transition> transitions = new HashSet<>();

        // Transition to open the door
        Transition transition =
                Transition.newBuilder(states).from(LiftDoorState.CLOSED)
                        .event(Cmd.OPEN).to(LiftDoorState.OPENED).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "Test", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }

    private SimpleStateMachine initWithHeartBeat(DoorStateListener listener) {

        Set<State> states = new HashSet<>();

        State state =
                States.newBuilder(LiftDoorState.OPENED)
                        .onEntry(this::blockDoor)
                        .heartBeatPeriod(50)
                        .heartBeatTimeoutTarget(LiftDoorState.CLOSED)
                        .heartBeatWorker(() -> closeable)
                        .timeout(1000)
                        .timeoutTarget(LiftDoorState.OPENED_AND_RINGING)
                        .build();
        states.add(state);

        // Door is opened and ringing, when door closes stop the bell
        state =
                States.newBuilder(LiftDoorState.OPENED_AND_RINGING)
                        .onEntry(() -> bell.ring())
                        .heartBeatPeriod(50)
                        .heartBeatTimeoutTarget(LiftDoorState.CLOSED)
                        .heartBeatWorker(() -> closeable)
                        .exitAction(() -> bell.stop())
                        .build();
        states.add(state);

        state = States.newBuilder(LiftDoorState.CLOSED).build();
        State initial = state;
        states.add(state);

        // Transition to open the door
        Set<Transition> transitions = new HashSet<>();
        Transition transition =
                Transition.newBuilder(states).from(LiftDoorState.CLOSED)
                        .event(Cmd.OPEN).to(LiftDoorState.OPENED).build();
        transitions.add(transition);
        fsm = new SimpleStateMachine(states, transitions, "Test", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }


    public void shutdown() {
        fsm.shutdown();
    }

    public boolean isRinging() {
        return bell.isRinging();
    }

    public void releaseDoor() {
        closeable = true;
    }

    public void blockDoor() {
        closeable = false;
    }


    static class Bell {

        private boolean ringing = false;

        void ring() {
            ringing = true;
        }

        void stop() {
            ringing = false;
        }

        boolean isRinging() {
            return ringing;
        }

    }

    static class DoorStateListener implements StateMachineListener {

        private final BlockingQueue<StateId> queue;

        DoorStateListener(BlockingQueue<StateId> queue) {
            this.queue = queue;
        }

        @Override
        public void onStateEntered(StateId state) {
            queue.offer(state);
        }

        @Override
        public void onStateExited(StateId state) {

        }
    }
}
