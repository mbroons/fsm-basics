package com.alu.oamp.fsm;

import java.util.concurrent.BlockingQueue;

/**
 * A listener for door state change events
 */
public class SimpleStateListener implements StateMachineListener {

    private final BlockingQueue<StateId> queue;

    SimpleStateListener(BlockingQueue<StateId> queue) {
        this.queue = queue;
    }

    @Override
    public void onStateEntered(StateId state) {
        queue.offer(state);
    }

    @Override
    public void onStateExited(StateId state) {
        // ignore
    }
}
