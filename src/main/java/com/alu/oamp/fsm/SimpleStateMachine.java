package com.alu.oamp.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple state machine.
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
 * The State machine supports states with heart beat.
 * <p/>
 * <p>
 * A state with heartbeat is active as long as the heartbeat worker reports
 * an ok status.
 * </p>
 * <p/>
 * <p>
 * When defining a state with heartbeat, one has to specify the exit polling period,
 * the heartbeat worker and the target state to enter on heartbeat error.
 * </p>
 */
public class SimpleStateMachine implements TimeoutListener {

    /**
     * State machine internal events
     */
    enum InternalEvent implements EventId {
        TIMEOUT,
        HEARTBEAT_ERROR
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SimpleStateMachine.class);

    private final Map<StateId, State> states = new HashMap<>();
    private final Map<StateId, Map<EventId, Transition>> transitionMap =
            new HashMap<>();
    private final EventProcessor eventProcessor;
    private State current;
    final String name;
    private final CopyOnWriteArrayList<StateMachineListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * A finite state machine.
     *
     * @param states      the state machine states
     * @param transitions the state machine transitions
     * @param fsmName     the state machine name
     * @param initial     the initial state
     */
    public SimpleStateMachine(Set<State> states, Set<Transition> transitions,
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

    public void addStateMachineListener(StateMachineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onTimeout() {
        fireEvent(InternalEvent.TIMEOUT);
    }

    @Override
    public void onHeartBeatError() {
        fireEvent(InternalEvent.HEARTBEAT_ERROR);
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
        listeners.clear();
    }

    /**
     * Fires an event on the state machine.
     *
     * @param eventId the event id
     */
    public void fireEvent(EventId eventId) {

        fireEvent(new Event(eventId));
    }

    /**
     * Fires an event on the state machine.
     *
     * @param eventId the event id
     * @param message the event message
     */
    public void fireEvent(EventId eventId, Object message) {

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
    private class EventProcessor extends AbstractEventLoop<Event> {

        /**
         * Creates a new notification processor.
         *
         * @param threadName the thread name.
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

                if (!transition.getCondition().isPresent() || transition.getCondition().get().getAsBoolean()) {
                    executeTransition(event, transition);
                }
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
                for (StateMachineListener listener : listeners) {
                    listener.onStateExited(current.getId());
                }
                current.onExit();
                transition.run(event);
                current = newState;
                LOGGER.debug("Entering state {}.", current);
                for (StateMachineListener listener : listeners) {
                    listener.onStateEntered(current.getId());
                }
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

        private final EventId eventId;
        private final Object message;

        /**
         * Creates a new state machine event.
         *
         * @param eventId the event id
         * @param message the event message
         */
        Event(EventId eventId, Object message) {
            this.eventId = eventId;
            this.message = message;
        }

        /**
         * Creates a new state machine event.
         *
         * @param eventId the event id
         */
        Event(EventId eventId) {
            this(eventId, null);
        }

        /**
         * Returns the event id.
         *
         * @return the event id.
         */
        EventId getId() {
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

    // for tests

    void setState(StateId stateId) {
        current = states.get(stateId);
    }

    StateId getState() {
        return current.getId();
    }

    void fireEventSync(EventId eventId) {

        fireEventSync(new Event(eventId));
    }

    void fireEventSync(EventId eventId, Object message) {

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
