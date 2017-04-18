package com.alu.oamp.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A state machine transition.
 *
 */
public class Transition {

	private static final Logger LOGGER =
		LoggerFactory.getLogger(Transition.class);

	private final State toState;
	private final EventId eventId;
	private final State fromState;
	private final Object action;

	/**
	 * Creates a new transition.
	 *
	 * @param fromState
	 *            the transition source state
	 * @param eventId
	 *            the transition event id
	 * @param toState
	 *            the transition target state
	 * @param action
	 *            the action to execute
	 */
	private Transition(State fromState, EventId eventId, State toState,
			Object action) {
		this.fromState = fromState;
		this.eventId = eventId;
		this.toState = toState;
		this.action = action;
	}

	/**
	 * Returns the transition target state.
	 *
	 * @return the transition target state.
	 */
	State getToState() {
		return toState;
	}

	/**
	 * Returns the transition event id.
	 *
	 * @return the transition event id
	 */
	EventId getEventId() {
		return eventId;
	}

	/**
	 * Returns the transition source state.
	 *
	 * @return the transition source state
	 */
	State getFromState() {
		return fromState;
	}

	@Override
	public String toString() {
		return "Transition [" + eventId + ", " + fromState + "]";
	}

	/**
	 * Executes the transition action.
	 *
	 * An action is either an instance of a Runnable or an instance of Action
	 * <T> (A Runnable with an input parameter).
	 *
	 * In case of an Action<T>, A class cast exception is logged if the event
	 * message sent does not match the action expected input parameter.
	 *
	 * @param event
	 *            the state machine event
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void run(SimpleStateMachine.Event event) {

		if (action != null) {
			try {
				if (action instanceof Runnable) {
					((Runnable) action).run();
				} else {
					((Action) action).run(event.getMessage());
				}
			} catch (Exception ex) {
				LOGGER.error("Exception on transition " + toString(), ex);
			}
		}
	}

	/**
	 * Returns a new transition builder.
	 *
	 * @param states
	 *            the state set.
	 * @return the builder
	 */
	@SuppressWarnings("synthetic-access")
	public static Builder newBuilder(Set<State> states) {
		Map<StateId, State> map = new HashMap<>();
		for (State state : states) {
			map.put(state.getId(), state);
		}
		return new Builder(map);
	}

	/**
	 * A transition builder.
	 *
	 */
	public static class Builder {

		private final Map<StateId, State> map;
		private State toState;
		private EventId eventId;
		private State fromState;
		private Object action;

		private Builder(Map<StateId, State> map) {
			this.map = map;
		}

		private void checkArgument(StateId stateId) {
			if (map.get(stateId) == null) {
				throw new IllegalArgumentException(
					"No state with id: " + stateId);
			}
		}

		/**
		 * Specifies the transition source state.
		 *
		 * @param state
		 *            the state
		 * @return the builder
		 */
		public Builder from(State state) {
			this.fromState = state;
			return this;
		}

		/**
		 * Specifies the transition target state.
		 *
		 * @param state
		 *            the state
		 * @return the builder
		 */
		public Builder to(State state) {
			this.fromState = state;
			return this;
		}

		/**
		 * Specifies the transition source state.
		 *
		 * The state must exist in the state set.
		 *
		 * @param stateId
		 *            the state id
		 * @return the builder
		 */
		public Builder from(StateId stateId) {
			checkArgument(stateId);
			this.fromState = map.get(stateId);
			return this;
		}

		/**
		 * Specifies the transition target state.
		 *
		 * The state must exist in the state set.
		 *
		 * @param stateId
		 *            the state id
		 * @return the builder
		 */
		public Builder to(StateId stateId) {
			checkArgument(stateId);
			this.toState = map.get(stateId);
			return this;
		}

		/**
		 * Specifies the transition event id.
		 *
		 * @param eventId
		 *            the event id
		 * @return the builder
		 */
		public Builder event(EventId eventId) {
			this.eventId = eventId;
			return this;
		}

		/**
		 * Specifies the transition action.
		 *
		 * The action does not take any input parameter.
		 *
		 * @param action
		 *            the action runnable
		 * @return the builder
		 */
		public Builder action(Runnable action) {
			this.action = action;
			return this;
		}

		/**
		 * Specifies the transition action.
		 *
		 * The action takes as an input parameter the message sent when firing
		 * the event.
		 *
		 * @param action
		 *            the action runnable
		 * @return the builder
		 */
		public Builder actionMessage(Action<?> action) {
			this.action = action;
			return this;
		}

		/**
		 * Builds the transition.
		 *
		 * @return the transition
		 */
		public Transition build() {
			checkNotNull(fromState, "fromState can't be null.");
			checkNotNull(eventId, "eventId can't be null.");
			return new Transition(fromState, eventId, toState, action);
		}

		private static void checkNotNull(Object object, String message) {
			if (object == null) {
				throw new IllegalArgumentException(message);
			}
		}
	}
}