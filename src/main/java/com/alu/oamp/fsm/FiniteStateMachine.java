package com.alu.oamp.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A finite state machine.
 * <p/>
 * <p>
 * The state machine can be described as a set of relations of the form:
 * <p/>
 * <p>
 * State(S) x Event(E) -> Action (A), State(S')
 * <p/>
 * <p>
 * A relation is interpreted as follow:
 * <p/>
 * <p>
 * If we are in state S and the event E occurs, we should perform the action A
 * and make a transition to the state S'.
 * <p/>
 * <p>
 * As much as possible, the implementation follows the SCXML specification,
 * (http://www.w3.org/TR/scxml/) which means:
 * <p/>
 * <p>
 * When an event E is received in state S, the associated transition (if any)
 * is looked up. If no transition is found for the couple (S, E) the state
 * machine stays in the current state. The event is not valid for the current
 * state. if a transition is found and a target state is defined, the state
 * machine executes the current state exit method, the transaction run method
 * and the new state entry method in sequence. the target state can be
 * identical to the current state (self transition).
 * <p/>
 * <p>
 * if a transition is found and the target state is null, the state machine
 * executes the transition execute method (internal transition) and stays in
 * the current state.
 * <p/>
 * <p>
 * The class is thread safe. The state machine is single threaded. Event
 * processing and action methods are executed on the worker thread. If a long
 * running task needs to be executed on entering a state, on exiting a state or
 * on running a transition, it is recommended to use a dedicated thread.
 * <p/>
 * <p>
 * The State machine supports states with timeout.
 * <p/>
 * <p>
 * When defining state with timeout, one has to specify the timeout value, the
 * action to invoke (optional) and the mandatory target state to enter when the
 * state times out.
 * </p>
 * <p/>
 * <p>
 * The State machine supports states with conditional exit.
 * <p/>
 * <p>
 * A state with conditional exit is active as long as the exit condition is
 * false. (For instance, in sc, a third application started state is active as
 * long as the pid is running.
 * </p>
 * <p/>
 * <p>
 * When defining a state with conditional exit, one has to specify the exit
 * condition polling period, the exit condition and the target state to enter
 * when the state exits.
 * </p>
 */
public class FiniteStateMachine implements TimeoutListener {

	/**
	 * State machine internal events
	 */
	enum InternalEvent {
		TIMEOUT,
		EXIT_MONITORING
	}

	private static final Logger LOGGER =
		LoggerFactory.getLogger(FiniteStateMachine.class);

	private final Map<Enum<?>, State> states = new HashMap<>();
	private final Map<Enum<?>, Map<Enum<?>, Transition>> transitionMap =
		new HashMap<>();
	private final EventProcessor eventProcessor;
	private State current;
	final String name;

	/**
	 * A finite state machine.
	 *
	 * @param states
	 *            the state machine states
	 * @param transitions
	 *            the state machine transitions
	 * @param fsmName
	 *            the state machine name
	 * @param initial
	 *            the initial state
	 */
	public FiniteStateMachine(Set<State> states, Set<Transition> transitions,
			String fsmName, State initial) {

		this.name = fsmName;
		if (transitions == null) {
			transitions = new HashSet<>();
		}

		Set<Transition> internalTransitions = new HashSet<>();
		for (State state : states) {
			this.states.put(state.getId(), state);
			transitionMap.put(state.getId(),
					new HashMap<>());

			if (state instanceof ActiveState) {
				((ActiveState) state).setActiveStateListener(this);
				internalTransitions
					.addAll(((ActiveState) state).getInternal(states));
			}
		}

		// Add internal transitions
		for (Transition trans : internalTransitions) {
			transitionMap.get(trans.getFromState().getId())
				.put(trans.getEventId(), trans);
		}

		// Add transitions
		for (Transition trans : transitions) {
			transitionMap.get(trans.getFromState().getId())
				.put(trans.getEventId(), trans);
		}
		this.current = initial;
		eventProcessor = new EventProcessor("FSM " + name);
	}

	@Override
	public void onTimeout() {
		fireEvent(InternalEvent.TIMEOUT);
	}

	@Override
	public void onExitMonitoring() {
		fireEvent(InternalEvent.EXIT_MONITORING);
	}

	/**
	 * shutdown the state machine
	 */
	public void shutdown() {
		for (State state : states.values()) {
			if (state instanceof ActiveState) {
				((ActiveState) state).shutdown();
			}
		}
		eventProcessor.shutdown();
	}

	/**
	 * Fires an event on the state machine.
	 *
	 * @param eventId
	 *            the event id
	 */
	public void fireEvent(Enum<?> eventId) {

		fireEvent(new Event(eventId));
	}

	/**
	 * Fires an event on the state machine.
	 *
	 * @param eventId
	 *            the event id
	 * @param message
	 *            the event message
	 */
	public void fireEvent(Enum<?> eventId, Object message) {

		fireEvent(new Event(eventId, message));
	}

	private void fireEvent(Event event) {

		if (!eventProcessor.isShutdown()) {
			eventProcessor.send(event);
		}
	}

	/**
	 * A control event processor.
	 */
	private class EventProcessor extends Actor<Event> {

		/**
		 * Creates a new notification processor.
		 *
		 * @param threadName
		 *            the thread name.
		 */
		public EventProcessor(String threadName) {
			super(threadName);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		protected void onMessage(Event event) {

			LOGGER.debug("Event {} is received", event);

			if (isTimeOut(event)) {
				LOGGER.error("State {} has timed out.", current);
			}

			Transition transition =
				transitionMap.get(current.getId()).get(event.getId());
			if (transition != null) {

				executeTransition(event, transition);
			} else {
				LOGGER.info("Event {} is ignored for state {}", event, current);
			}
		}

		private boolean isTimeOut(Event event) {
			return event.getId() == InternalEvent.TIMEOUT;
		}

		@SuppressWarnings("synthetic-access")
		private void executeTransition(Event event, Transition transition) {
			State newState = transition.getToState();
			if (newState != null) {

				LOGGER.debug("Leaving state {}.", current);
				current.onExit();
				transition.run(event);
				current = newState;
				LOGGER.debug("Entering state {}.", current);
				current.onEntry();
			} else {
				transition.run(event);
			}
		}
	}

	/**
	 * An event as used by the state machine.
	 */
	static class Event {

		private final Enum<?> eventId;
		private final Object message;

		/**
		 * Creates a new state machine event.
		 *
		 * @param eventId
		 *            the event id
		 * @param message
		 *            the event message
		 */
		Event(Enum<?> eventId, Object message) {
			this.eventId = eventId;
			this.message = message;
		}

		/**
		 * Creates a new state machine event.
		 *
		 * @param eventId
		 *            the event id
		 */
		Event(Enum<?> eventId) {
			this(eventId, null);
		}

		/**
		 * Returns the event id.
		 *
		 * @return the event id.
		 */
		Enum<?> getId() {
			return eventId;
		}

		/**
		 * Returns the event message.
		 *
		 * @return the event message.
		 */
		Object getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "[" + eventId + "]";
		}
	}

	// For tests
	public void disableTimers(TimerProvider timerProvider) {

		for (State state : states.values()) {
			if (state instanceof StateWithHeartBeat) {
				((StateWithHeartBeat) state).setProvider(timerProvider);
			}
			if (state instanceof StateWithTimeout) {
				((StateWithTimeout) state).setProvider(timerProvider);
			}
		}
	}

	public void setState(Enum<?> stateId) {
		current = states.get(stateId);
	}

	public Enum<?> getState() {
		return current.getId();
	}

	public void fireSyncTimeout() {
		fireEventSync(InternalEvent.TIMEOUT);
	}

	public void fireSyncExitMonitoring() {
		fireEventSync(InternalEvent.EXIT_MONITORING);
	}

	public void fireEventSync(Enum<?> eventId) {

		fireEventSync(new Event(eventId));
	}

	public void fireEventSync(Enum<?> eventId, Object message) {

		fireEventSync(new Event(eventId, message));
	}

	private void fireEventSync(Event event) {

		if (transitionMap.get(current.getId()).get(event.getId()) == null) {
			throw new IllegalStateException(
				"No transition found for event " + event);
		}
		eventProcessor.onMessage(event);
	}

}
