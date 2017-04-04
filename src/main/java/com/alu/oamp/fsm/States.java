package com.alu.oamp.fsm;

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
	public static Builder newBuilder(Enum<?> stateId) {
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
		
		private final Enum<?> stateId;
		private Runnable onEntry;
		private Runnable onExit;

        private long timeout;
        private Enum<?> timeoutStateId;
        private Runnable onTimeout;

        private long period;
        private ExitCondition exitCondition;
        private Runnable exitAction;
        private Enum<?> exitStateId;

		/**
		 * Creates a new state builder.
		 * @param stateId the state id
		 */
		private Builder(Enum<?> stateId) {
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
		 * @param onTimeout the timeout action
		 * @return the state builder
		 */
		public Builder onTimeout(Runnable onTimeout) {
			this.onTimeout = onTimeout;
			return this;
		}
		
		/**
		 * Specifies the target state when a state times out.
		 * 
		 * @param targetStateId the target state on timeout
		 * @return the state builder
		 */
		public Builder timeoutTarget(Enum<?> targetStateId) {
			this.timeoutStateId = targetStateId;
			return this;
		}

        /**
         * Specifies the monitoring period for a state with monitoring.
         *
         * @param period the monitoring period
         * @return the state builder
         */
        public Builder monitoring(long period) {
            this.period = period;
            return this;
        }

        /**
         * Specifies the exit monitoring condition.
         *
         * @param exitCondition the exit monitoring condition
         * @return the state builder
         */
        public Builder exitCondition(ExitCondition exitCondition) {
            this.exitCondition = exitCondition;
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
         * Specifies the target state when the state is exited due
         * to monitoring.
         *
         * @param exitStateId the target state
         * @return the state builder
         */
        public Builder monitoringTarget(Enum<?> exitStateId) {
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
                checkNotNull(exitCondition, "Monitored state exit condition can't be null");
                built = new StateWithExitCondition(state, period, exitStateId, exitCondition, exitAction);
            }
			
			if (timeout != 0) {
				// This is a state with timeout
				checkNotNull(timeoutStateId, "Target state on timeout can't be null");
                built = new StateWithTimeout(built, timeout, onTimeout, timeoutStateId);
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
