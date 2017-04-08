package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;

/**
 * A state with heart beat.
 * <p/>
 * <p>
 * The state remains active as long as the heart beat reports an ok status
 * </p>
 * <p/>
 * The state is active as long as the exit condition is not true
 * <p/>
 */
public class StateWithHeartBeat extends AbstractActiveState {

	private final long period;
	private final Enum<?> exitStateId;
	private final BooleanSupplier heartBeatWorker;
	private final Runnable exitAction;

	/**
	 * Creates a new state with heart beat.
	 *
	 * @param innerState
	 *            the inner state.
	 * @param period
	 *            the heart beat polling period
	 * @param worker
	 *            the heart beat operation
	 * @param exitAction
	 *            the action to perform on exit
	 * @param exitStateId
	 *            the target state to enter.
	 */
	StateWithHeartBeat(State innerState, long period, Enum<?> exitStateId,
					   BooleanSupplier worker, Runnable exitAction) {
        super(innerState);
        this.period = period;
        this.heartBeatWorker = worker;
        this.exitAction = exitAction;
        this.exitStateId = exitStateId;
        provider = () -> new Timer("Monitor " + state.toString());
    }

	@Override
	public void onEntry() {

		state.onEntry();
		timer = provider.get();
		TimerTask runHeartBeat = new TimerTask() {
			public void run() {
				if (heartBeatWorker.getAsBoolean()) {
					listener.onExitMonitoring();
					timer.cancel();
				}
			}
		};
		timer.schedule(runHeartBeat, period, period);
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