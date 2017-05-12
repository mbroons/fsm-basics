package com.alu.oamp.fsm;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Utility class to create states.
 *
 *
 */
public class States {
	
	private States() {
	}
	
	/**
	 * Creates a new state builder.
	 * @param stateId the state id
	 * @return a new state builder.
	 */
	public static Builder state(StateId stateId) {
		return new Builder(stateId);
	}
	
	/**
	 * A state builder.
	 * 
	 * When building a state, a state id is mandatory. on entry and on exit
	 * methods are optional.
	 * 
	 */
	public static class Builder {
		
		private final StateId stateId;
		private Runnable onEntry;
		private Runnable onExit;

        private Optional<Timeout> timeout = Optional.empty();

        private long period;
        private BooleanSupplier heartBeatError;
        private Runnable exitAction;
        private StateId exitStateId;

		/**
		 * Creates a new state builder.
		 * @param stateId the state id
		 */
		private Builder(StateId stateId) {
			checkNotNull(stateId, "State id can't be null");
			this.stateId = stateId;
		}

		/**
		 * Specifies the method to be called when entering the state.
		 * @param onEntry the on entry runnable
		 * @return the state builder
		 */
		public Builder onEntry(Runnable onEntry) {
			this.onEntry = onEntry;
			return this;
		}
		
		/**
		 * Specifies the method to be called when exiting the state.
		 * @param onExit the on exit runnable
		 * @return the state builder
		 */
		public Builder onExit(Runnable onExit) {
			this.onExit = onExit;
			return this;
		}
		
		/**
		 * Specifies the state timeout.
		 * 
		 * @param timeout the timeout
		 * @return the state builder
		 */
		public Builder timeout(Timeout timeout) {
			this.timeout = Optional.ofNullable(timeout);
			return this;
		}

        /**
         * Specifies the monitoring period for a state with monitoring.
         *
         * @param period the monitoring period
         * @return the state builder
         */
        public Builder heartBeatPeriod(long period) {
            this.period = period;
            return this;
        }

        /**
         * Specifies the heart beat condition
         *
         * @param condition the heart beat condition.
         * @return the state builder
         */
        public Builder heartBeatError(BooleanSupplier condition) {
            this.heartBeatError = condition;
            return this;
        }

        /**
         * Specifies the action to invoke when state is exited due
         * to monitoring
         *
         * @param action the exit monitoring action
         * @return the state builder
         */
        public Builder exitAction(Runnable action) {
            this.exitAction = action;
            return this;
        }

        /**
         * Specifies the target state when the state has exited due to time out on heart beat.
         *
         * @param exitStateId the target state
         * @return the state builder
         */
        public Builder heartBeatTimeoutTarget(StateId exitStateId) {
            this.exitStateId = exitStateId;
            return this;
        }
		

		/**
		 * Builds the state.
		 * @return the new state.
		 */
		public State build() {
            BaseState state = new BaseState(stateId, onEntry, onExit);
            State built = state;

            if (period != 0) {
                // This is a state with monitoring
                checkNotNull(exitStateId, "Target state for monitored state can't be null");
                checkNotNull(heartBeatError, "State with heart beat can't have null heart beat worker");
                built = new HeartbeatAbleState(state, period, exitStateId, heartBeatError, exitAction);
            }

			if (timeout.isPresent()) {
                built = new TimeoutAbleState(built, timeout.get());
			}
			return built;
		}
		
		private static void checkNotNull(Object object, String message) {
			if (object == null) {
				throw new IllegalArgumentException(message);
			}
		}
	}



}
