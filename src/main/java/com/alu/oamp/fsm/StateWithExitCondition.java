package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A state with exit condition.
 * <p/>
 * <p>
 * A state with exit condition is active until the exit condition becomes true
 * </p>
 * <p/>
 * The state is active as long as the exit condition is not true
 * <p/>
 */
public class StateWithExitCondition extends AbstractActiveState {

	private static final Logger LOGGER =
		LoggerFactory.getLogger(StateWithExitCondition.class);

	private final long period;
	private final Enum<?> exitStateId;
	private final ExitCondition exitCondition;
	private final Runnable exitAction;

	/**
	 * Creates a new state with exit condition.
	 *
	 * @param innerState
	 *            the inner state.
	 * @param period
	 *            the exit condition polling period
	 * @param exitCondition
	 *            the exit condition
	 * @param exitAction
	 *            the action to perform on exit
	 * @param exitStateId
	 *            the target state to enter.
	 */
	StateWithExitCondition(State innerState, long period, Enum<?> exitStateId,
			ExitCondition exitCondition, Runnable exitAction) {
		super(innerState);
		this.period = period;
		this.exitCondition = exitCondition;
		this.exitAction = exitAction;
		this.exitStateId = exitStateId;
		provider = new TimerProvider() {
			@Override
			public Timer get() {
				return new Timer("Monitor " + state.toString());
			}
		};
	}

	@Override
	public void onEntry() {

		state.onEntry();
		timer = provider.get();
		TimerTask monitoring = new TimerTask() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				LOGGER.debug("Monitoring using timer: {}", timer);
				if (exitCondition.apply()) {
					listener.onExitMonitoring();
					timer.cancel();
				}
			}
		};
		timer.schedule(monitoring, period, period);
	}

	@Override
	public Set<Transition> getInternal(Set<State> states) {

		Set<Transition> transitions = getInnerStateTransitions(states);

		// add exit monitoring transition
		transitions.add(Transition.newBuilder(states).from(getId())
			.event(FiniteStateMachine.InternalEvent.EXIT_MONITORING)
			.to(exitStateId).action(exitAction).build());
		return transitions;
	}
}