package com.alu.oamp.fsm;

/**
 * A state heartbeat
 */
public class Heartbeat {

    private final long period;
    private final Runnable action;

    /**
     * Creates an heartbeat
     * @param period the heartbeat period
     * @param action the action performed when exiting the state
     */
    private Heartbeat(long period, Runnable action) {
        this.period = period;
        this.action = action;
    }

    public long getPeriod() {
        return period;
    }

    public Runnable getAction() {
        return action;
    }

    public static Builder buildWith() {
        return new Builder();
    }

    public static class Builder {

        private long period;
        private Runnable action;

        /**
         * Specifies the heart beat period.
         *
         * @param period the period
         * @return the state builder
         */
        public Builder period(long period) {
            this.period = period;
            return this;
        }

        /**
         * Specifies the heartbeat action.
         *
         * @param action the heartbeat action
         * @return the state builder
         */
        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        /**
         * Builds the heartbeat.
         * @return the new heartbeat.
         */
        public Heartbeat build() {
            return new Heartbeat(period, action);
        }
    }
}