package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple state machine simulating a lift door.
 * <p/>
 * States are [Opened; Closed; Opened and Ringing]
 * and 1 command for the door: Open the door.
 * <p/>
 * Several scenario are tested.
 */
public class FiniteStateMachineTest {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FiniteStateMachineTest.class);

    private FiniteStateMachine fsm;
    private BlockingQueue<LiftDoorState> queue =
            new LinkedBlockingQueue<>();
    private Cell cell = new Cell();
    private Bell bell = new Bell();

    enum Cmd {
        OPEN
    }

    enum LiftDoorState {

        OPENED,
        OPENED_AND_RINGING,
        CLOSED
    }

    private void initFsmWithTimeout() {

        Set<State> states = new HashSet<>();

        // The door stays opened for 500 ms and closes itself.
        State state =
                States.newBuilder(LiftDoorState.OPENED).onEntry(new Runnable() {

                    @Override
                    public void run() {
                        queue.offer(LiftDoorState.OPENED);
                    }
                }).timeout(500).timeoutTarget(LiftDoorState.CLOSED).build();
        states.add(state);

        state = States.newBuilder(LiftDoorState.CLOSED).onEntry(new Runnable() {

            @Override
            public void run() {
                queue.offer(LiftDoorState.CLOSED);
            }
        }).build();
        State initial = state;
        states.add(state);

        Set<Transition> transitions = new HashSet<>();

        // Transition to open the door
        Transition transition =
                Transition.newBuilder(states).from(LiftDoorState.CLOSED)
                        .event(Cmd.OPEN).to(LiftDoorState.OPENED).build();
        transitions.add(transition);

        fsm = new FiniteStateMachine(states, transitions, "Test", initial);
    }

    private void initFsmWithMonitoring() {

        Set<State> states = new HashSet<>();

        // The opened state is exited when cell does not detect someone
        // If Door is opened for more than one second, ring the bell
        ExitCondition condition = new ExitCondition() {
            @Override
            public boolean apply() {
                LOGGER.info("cell is: {}", cell);
                return cell.isOff();
            }
        };
        State state =
                States.newBuilder(LiftDoorState.OPENED).onEntry(new Runnable() {

                    @Override
                    public void run() {
                        cell.setOn();
                        queue.offer(LiftDoorState.OPENED);
                    }
                }).monitoring(50).monitoringTarget(LiftDoorState.CLOSED).exitCondition(condition).timeout(1000)
                        .timeoutTarget(LiftDoorState.OPENED_AND_RINGING).build();
        states.add(state);

        // Door is opened and ringing, when door closes stop the bell
        Runnable exitAction = new Runnable() {
            @Override
            public void run() {
                bell.stop();
            }
        };
        state =
                States.newBuilder(LiftDoorState.OPENED_AND_RINGING).onEntry(new Runnable() {

                    @Override
                    public void run() {
                        bell.ring();
                        queue.offer(LiftDoorState.OPENED_AND_RINGING);
                    }
                }).monitoring(50).monitoringTarget(LiftDoorState.CLOSED).exitCondition(condition).exitAction(exitAction)
                        .build();
        states.add(state);

        state = States.newBuilder(LiftDoorState.CLOSED).onEntry(new Runnable() {

            @Override
            public void run() {
                queue.offer(LiftDoorState.CLOSED);
            }
        }).build();
        State initial = state;
        states.add(state);

        // Transition to open the door
        Set<Transition> transitions = new HashSet<>();
        Transition transition =
                Transition.newBuilder(states).from(LiftDoorState.CLOSED)
                        .event(Cmd.OPEN).to(LiftDoorState.OPENED).build();
        transitions.add(transition);

        fsm = new FiniteStateMachine(states, transitions, "Test", initial);
    }

    private void initFsmForActiveState() {

        Set<State> states = new HashSet<>();

        // The opened state is exited when cell does not detect someone
        // If Door is opened for more than one second, ring the bell
        ExitCondition cellIsOff = new ExitCondition() {
            @Override
            public boolean apply() {
                LOGGER.info("cell is: {}", cell);
                return cell.isOff();
            }
        };
        State state =
                States.newBuilder(LiftDoorState.OPENED).onEntry(new Runnable() {

                    @Override
                    public void run() {
                        cell.setOn();
                        queue.offer(LiftDoorState.OPENED);
                    }
                }).monitoring(10).monitoringTarget(LiftDoorState.CLOSED).exitCondition(cellIsOff).timeout(1000)
                        .timeoutTarget(LiftDoorState.CLOSED).build();
        states.add(state);

        state = States.newBuilder(LiftDoorState.CLOSED).onEntry(new Runnable() {

            @Override
            public void run() {
                queue.offer(LiftDoorState.CLOSED);
            }
        }).build();
        State initial = state;
        states.add(state);

        // Transition to open the door
        Set<Transition> transitions = new HashSet<>();
        Transition transition =
                Transition.newBuilder(states).from(LiftDoorState.CLOSED)
                        .event(Cmd.OPEN).to(LiftDoorState.OPENED).build();
        transitions.add(transition);


        fsm = new FiniteStateMachine(states, transitions, "Test", initial);
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
    public void testFsmWithMonitoring() throws InterruptedException {
        initFsmWithMonitoring();

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
            if (on) return "On";
            else return "Off";
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
}
