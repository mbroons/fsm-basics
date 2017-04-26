package com.alu.oamp.fsm;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple state machine simulating a lift door.
 * <p>
 * States are [Opened; Closed; Opened_and_Ringing]
 * and 1 command for the door: Open the door.
 * <p>
 * Several scenario are tested.
 */
public class LiftDoorTest {

    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();
    private LiftDoor liftDoor;

    @AfterMethod
    public void tearDown() {
        liftDoor.shutdown();
    }

    @Test
    public void testTimeout() throws InterruptedException {

        liftDoor = LiftDoor.newSimpleLiftDoor(new LiftDoor.DoorStateListener(queue));
        liftDoor.fireEvent(LiftDoor.Cmd.OPEN);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.OPENED);
        Assert.assertEquals(queue.poll(1000, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.CLOSED);
    }

    @Test
    public void testHeartBeat() throws InterruptedException {

        liftDoor = LiftDoor.newLiftDoorWithHeardBeat(new LiftDoor.DoorStateListener(queue));

        // Open the door
        liftDoor.fireEvent(LiftDoor.Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.OPENED);

        // Close the door
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.CLOSED);

        // open the door
        liftDoor.fireEvent(LiftDoor.Cmd.OPEN);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.OPENED);

        liftDoor.lockDoor();

        // Wait 1200, bell rings
        TimeUnit.MILLISECONDS.sleep(1200);
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.OPENED_AND_RINGING);
        Assert.assertTrue(liftDoor.isRinging());

        // Release the door, it will close
        liftDoor.releaseDoor();
        Assert.assertEquals(queue.poll(200, TimeUnit.MILLISECONDS),
                LiftDoor.LiftDoorState.CLOSED);
        Assert.assertFalse(liftDoor.isRinging());
    }
}
