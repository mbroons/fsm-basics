package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;

/**
 * <p>
 * An heartbeat able state implements an heart beat mechanism to check whether the state should remain active or not.
 * </p>
 * <p>
 * The client application provides an heart beat error function that will be regularly evaluated. As soon as the
 * heart beat function reports an error, the state is exited and the state identified by the target state id
 * is entered.
 * </p>
 */
public class HeartbeatAbleState extends AbstractTimedState {

	private final long period;
	private final StateId targetStateId;
	private final BooleanSupplier heartBeatError;
	private final Runnable exitAction;

	/**
	 * Creates a new state with heart beat.
	 *
	 * @param innerState
	 *            the inner state.
	 * @param period
	 *            the heart beat polling period
	 * @param heartBeatError
	 *            the heart beat error.
	 * @param exitAction
	 *            the action to perform on exit
	 * @param targetStateId
	 *            the target state on heart beat error..
	 */
	HeartbeatAbleState(State innerState, long period, StateId targetStateId,
					   BooleanSupplier heartBeatError, Runnable exitAction) {
        super(innerState);
        this.period = period;
        this.heartBeatError = heartBeatError;
        this.exitAction = exitAction;
        this.targetStateId = targetStateId;
        provider = () -> new Timer("Monitor " + state.toString());
    }

	@Override
	public void onEntry() {

		state.onEntry();
		timer = provider.get();
		TimerTask runHeartBeat = new TimerTask() {
			public void run() {
				if (heartBeatError.getAsBoolean()) {
					listener.onHeartBeatError();
					timer.cancel();
				}
			}
		};
		timer.schedule(runHeartBeat, period, period);
	}

	@Override
	public Set<Transition> getInternal(Set<State> states) {

		Set<Transition> transitions = getInnerStateTransitions(states);

		// add heart beat exit condition
		transitions.add(Transition.newBuilder(states).from(getId())
			.event(SimpleStateMachine.InternalEvent.HEARTBEAT_ERROR)
			.to(targetStateId).action(exitAction).build());
		return transitions;
	}
}