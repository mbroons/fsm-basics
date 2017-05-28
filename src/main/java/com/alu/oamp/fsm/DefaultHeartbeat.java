package com.alu.oamp.fsm;

/**
 * A state heartbeat
 */
public class DefaultHeartbeat {

    private final long period;
    private final Runnable heartbeatAction;

    /**
     * Creates an heartbeat
     * @param period the heartbeat period
     * @param heartbeatAction the action performed when exiting the state
     */
    private DefaultHeartbeat(long period, Runnable heartbeatAction) {
        this.period = period;
        this.heartbeatAction = heartbeatAction;
    }

    public long getPeriod() {
        return period;
    }

    public Runnable getHeartbeatAction() {
        return heartbeatAction;
    }

    public static Builder buildWith() {
        return new Builder();
    }

    public static class Builder {

        private long period;
        private Runnable heartbeatAction;

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
         * Specifies the action when exiting the state
         *
         * @param action the exit action
         * @return the state builder
         */
        public Builder action(Runnable action) {
            this.heartbeatAction = action;
            return this;
        }

        /**
         * Builds the heartbeat.
         * @return the new heartbeat.
         */
        public DefaultHeartbeat build() {
            return new DefaultHeartbeat(period, heartbeatAction);
        }
    }
}