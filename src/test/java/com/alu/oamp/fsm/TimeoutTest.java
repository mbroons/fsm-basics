package com.alu.oamp.fsm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.alu.oamp.fsm.States.state;
import static com.alu.oamp.fsm.Timeout.buildWith;

/**
 * This is a simple state machine for timeout feature validation
 */
public class TimeoutTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutTest.class);

    private SimpleStateMachine fsm;
    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();

    enum Cmd implements EventId {
        TO_STATE_2,
        LONG_RUNNING_TASK
    }

    enum State implements StateId {

        STATE_1,
        STATE_2
    }

    @BeforeClass
    public void setup() {
        fsm =  init(new SimpleStateListener(queue));
    }

    public void tearDown() {
        fsm.shutdown();
    }

    public void fireEvent(Cmd cmd) {
        fsm.fireEvent(cmd);
    }

    private SimpleStateMachine init(SimpleStateListener listener) {

        Set<com.alu.oamp.fsm.State> states = new HashSet<>();

        com.alu.oamp.fsm.State state = state(State.STATE_1)
                .build();
        states.add(state);
        com.alu.oamp.fsm.State initial = state;


        state = state(State.STATE_2)
                .timeout(buildWith().timeout(1000).target(State.STATE_1).build())
                .build();
        states.add(state);

        Set<Transition> transitions = new HashSet<>();

        // Transition to open the door
        Transition transition =
                Transition.newBuilder(states).from(State.STATE_1)
                        .event(Cmd.TO_STATE_2).to(State.STATE_2).build();

        transitions.add(transition);

        Runnable sleepAction = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
                LOGGER.info("long task has completed");

            } catch (InterruptedException e) {
                LOGGER.info("I have been terminated folks");
            }
        };
        transition =
                Transition.newBuilder(states).from(State.STATE_2)
                        .event(Cmd.LONG_RUNNING_TASK).action(sleepAction).build();
        transitions.add(transition);

        fsm = new SimpleStateMachine(states, transitions, "TimeOut Test", initial);
        fsm.addStateMachineListener(listener);
        return fsm;
    }

    @Test
    public void test_long_running_task_is_interrupted() throws InterruptedException {

        fsm.fireEvent(Cmd.TO_STATE_2);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS), State.STATE_2);
        fsm.fireEvent(Cmd.LONG_RUNNING_TASK);

        // even though we are running a long task, the state should timeout
        Assert.assertEquals(queue.poll(1100, TimeUnit.MILLISECONDS), State.STATE_1);

    }
}
