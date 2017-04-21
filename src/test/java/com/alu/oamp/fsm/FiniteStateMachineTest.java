package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple state machine simulating a lift door.
 * <p>
 * States are [Opened; Closed; Opened_and_Ringing]
 * and 1 command for the door: Open the door.
 * <p>
 * Several scenario are tested.
 */
public class FiniteStateMachineTest {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FiniteStateMachineTest.class);

    private SimpleStateMachine fsm;
    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();
    private Cell cell = new Cell();
    private Bell bell = new Bell();

    @AfterMethod
    public void tearDown() {
        fsm.shutdown();
    }

    enum Cmd implements EventId {
        OPEN
    }

    enum LiftDoorState implements StateId {

        OPENED,
        OPENED_AND_RINGING,
        CLOSED
    }

    private void initFsmWithTimeout() {

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
        fsm.addStateMachineListener(new DoorStateListener(queue));
    }

    private void initFsmWithHeartBeat() {

        Set<State> states = new HashSet<>();

        State state =
                States.newBuilder(LiftDoorState.OPENED)
                        .onEntry(() -> cell.setOn())
                        .heartBeatPeriod(50)
                        .heartBeatTimeoutTarget(LiftDoorState.CLOSED)
                        .heartBeatWorker(() -> cell.isOff())
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
                        .heartBeatWorker(() -> cell.isOff())
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
        fsm.addStateMachineListener(new DoorStateListener(queue));
    }

    private void initFsmForActiveState() {

        Set<State> states = new HashSet<>();

        State state = States.newBuilder(LiftDoorState.OPENED)
                .onEntry(() -> cell.setOn())
                .onExit(() -> LOGGER.info("Exiting Opened state..."))
                .heartBeatPeriod(10)
                .heartBeatTimeoutTarget(LiftDoorState.CLOSED)
                .heartBeatWorker(() -> cell.isOff())
                .timeout(1000)
                .timeoutTarget(LiftDoorState.CLOSED).build();
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
        fsm.addStateMachineListener(new DoorStateListener(queue));
    }

    @Test
    public void testFsmWithTimeout() throws InterruptedException {
        initFsmWithTimeout();
        fsm.fireEvent(Cmd.OPEN);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS),
                LiftDoorState.OPENED);
        Assert.assertEquals(queue.poll(1000, TimeUnit.MILLISECONDS),
                LiftDoorState.CLOSED);
    }

    @Test
    public void testFsmWithHeartBeat() throws InterruptedException {
        initFsmWithHeartBeat();

        // Open the door
        fsm.fireEvent(Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.OPENED);

        // Close the door
        cell.setOff();
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.CLOSED);

        // open the door
        fsm.fireEvent(Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.OPENED);

        // Wait 1200, bell rings
        TimeUnit.MILLISECONDS.sleep(1200);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.OPENED_AND_RINGING);
        Assert.assertTrue(bell.isRinging());

        // Close the door
        cell.setOff();
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.CLOSED);
        Assert.assertFalse(bell.isRinging());

    }

    @Test
    public void testActiveStateShutdown() throws InterruptedException {
        initFsmForActiveState();

        // Open the door
        fsm.fireEvent(Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorState.OPENED);

        Assert.assertEquals(queue.poll(5000, TimeUnit.MILLISECONDS),
                LiftDoorState.CLOSED);

    }


    static class Cell {

        private boolean on;

        boolean isOff() {
            return !on;
        }

        void setOn() {
            on = true;
        }

        void setOff() {
            on = false;
        }

        @Override
        public String toString() {
            if (on) {
                return "On";
            } else {
                return "Off";
            }
        }
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
