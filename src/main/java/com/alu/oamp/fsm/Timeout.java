package com.alu.oamp.fsm;

/**
 * A state timeout
 */
public class Timeout {

    private final long timeout;
    private final Runnable action;
    private final StateId targetStateId;


    private Timeout(long timeout, Runnable timeoutAction, StateId targetStateId) {
        this.timeout = timeout;
        this.action = timeoutAction;
        this.targetStateId = targetStateId;
    }

    public long getTimeout() {
        return timeout;
    }

    public Runnable getAction() {
        return action;
    }

    public StateId getTargetStateId() {
        return targetStateId;
    }

    public static Builder newTimeout() {
        return new Builder();
    }

    public static class Builder {

        private long timeout;
        private Runnable timeoutAction;
        private StateId targetStateId;

        /**
         * Specifies the state timeout value for a state with timeout.
         *
         * @param timeout the timeout value
         * @return the state builder
         */
        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Specifies the action to invoke when a state times out.
         *
         * @param timeoutAction the timeout action
         * @return the state builder
         */
        public Builder onTimeout(Runnable timeoutAction) {
            this.timeoutAction = timeoutAction;
            return this;
        }

        /**
         * Specifies the target state when a state times out.
         *
         * @param targetStateId the target state on timeout
         * @return the state builder
         */
        public Builder target(StateId targetStateId) {
            this.targetStateId = targetStateId;
            return this;
        }

        /**
         * Builds the timeout.
         * @return the new timeout.
         */
        public Timeout build() {
            return new Timeout(timeout, timeoutAction, targetStateId);
        }
    }
}