package com.alu.oamp.fsm;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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


	private final Heartbeat heartbeat;

	/**
	 * Creates a new state with heart beat.
	 *
	 * @param innerState
	 *            the inner state.
	 * @param heartbeat
	 *            the heart beat specification
	 */
	HeartbeatAbleState(State innerState, Heartbeat heartbeat) {
        super(innerState);
        this.heartbeat = heartbeat;
        provider = () -> new Timer("Monitor " + state.toString());
    }

	@Override
	public void onEntry() {

		state.onEntry();
		timer = provider.get();
		TimerTask runHeartBeat = new TimerTask() {
			public void run() {
				if (heartbeat.getHeartBeatError().getAsBoolean()) {
					listener.onHeartBeat();
					timer.cancel();
				}
			}
		};
		timer.schedule(runHeartBeat, heartbeat.getPeriod(), heartbeat.getPeriod());
	}

	@Override
	public Set<Transition> getInternal(Set<State> states) {

		Set<Transition> transitions = getInnerStateTransitions(states);

		// add heart beat exit condition
		transitions.add(Transition.newBuilder(states).from(getId())
			.event(SimpleStateMachine.InternalEvent.HEARTBEAT)
			.to(heartbeat.getTargetStateId()).action(heartbeat.getExitAction()).build());
		return transitions;
	}
}