package com.alu.oamp.fsm;

import java.util.Optional;

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
		private Optional<Heartbeat> heartbeat = Optional.empty();

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
         * Specifies the state timeout.
         *
         * @param heartbeat the heartbeat
         * @return the state builder
         */
        public Builder heartbeat(Heartbeat heartbeat) {
            this.heartbeat = Optional.ofNullable(heartbeat);
            return this;
        }

		/**
		 * Builds the state.
		 * @return the new state.
		 */
		public State build() {
            State built = new BaseState(stateId, onEntry, onExit);

            if (heartbeat.isPresent()) {
                built = new HeartbeatAbleState(built, heartbeat.get());
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
