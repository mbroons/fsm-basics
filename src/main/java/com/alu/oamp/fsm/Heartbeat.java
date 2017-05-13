package com.alu.oamp.fsm;

import java.util.function.BooleanSupplier;

/**
 * A state heartbeat
 */
public class Heartbeat {

    private final long period;
    private final BooleanSupplier heartBeatError;
    private final Runnable exitAction;
    private final StateId targetStateId;

    /**
     * Creates an heartbeat
     * @param period the heartbeat period
     * @param heartBeatError the heartbeat error function
     * @param exitAction the action performed when exiting the state
     * @param targetStateId the target state id
     */
    private Heartbeat(long period, BooleanSupplier heartBeatError, Runnable exitAction, StateId targetStateId) {
        this.period = period;
        this.heartBeatError = heartBeatError;
        this.exitAction = exitAction;
        this.targetStateId = targetStateId;
    }

    public long getPeriod() {
        return period;
    }

    public BooleanSupplier getHeartBeatError() {
        return heartBeatError;
    }

    public Runnable getExitAction() {
        return exitAction;
    }

    public StateId getTargetStateId() {
        return targetStateId;
    }

    public static Builder buildWith() {
        return new Builder();
    }

    public static class Builder {

        private long period;
        private BooleanSupplier heartBeatError;
        private Runnable exitAction;
        private StateId targetStateId;

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
         * Specifies the heart beat error function
         *
         * @param condition the heart beat error function.
         * @return the state builder
         */
        public Builder error(BooleanSupplier condition) {
            this.heartBeatError = condition;
            return this;
        }

        /**
         * Specifies the action when exiting the state
         *
         * @param action the exit action
         * @return the state builder
         */
        public Builder exitAction(Runnable action) {
            this.exitAction = action;
            return this;
        }

        /**
         * Specifies the target state when the state has exited.
         *
         * @param targetStateId the target state
         * @return the state builder
         */
        public Builder targetStateId(StateId targetStateId) {
            this.targetStateId = targetStateId;
            return this;
        }

        /**
         * Builds the heartbeat.
         * @return the new heartbeat.
         */
        public Heartbeat build() {
            return new Heartbeat(period, heartBeatError, exitAction, targetStateId);
        }
    }
}