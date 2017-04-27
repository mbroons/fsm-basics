package com.alu.oamp.fsm;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is a state machine simulating a lift door with a heart beat state
 */
public class LiftDoorWithHeartBeatTest {

    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();
    private LiftDoorWithHeartBeat liftDoor;

    @AfterMethod
    public void tearDown() {
        liftDoor.shutdown();
    }

    @Test
    public void testHeartBeat() throws InterruptedException {

        liftDoor = LiftDoorWithHeartBeat.newDoor(new DoorStateListener(queue));

        // Open the door
        liftDoor.fireEvent(LiftDoorWithHeartBeat.Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorWithHeartBeat.State.OPENED);

        // Wait, the door closes
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorWithHeartBeat.State.CLOSED);

        // open the door
        liftDoor.fireEvent(LiftDoorWithHeartBeat.Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorWithHeartBeat.State.OPENED);

        // presence is detected
        liftDoor.fireEvent(LiftDoorWithHeartBeat.Cmd.PRESENCE);

        // Wait, bell rings
        TimeUnit.MILLISECONDS.sleep(1200);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorWithHeartBeat.State.OPENED_AND_RINGING);
        Assert.assertTrue(liftDoor.isRinging());

        // presence is no more detected
        liftDoor.fireEvent(LiftDoorWithHeartBeat.Cmd.ABSENCE);

        // The door closes itself
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoorWithHeartBeat.State.CLOSED);
        Assert.assertFalse(liftDoor.isRinging());
    }
}
