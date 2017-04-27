package com.alu.oamp.fsm;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Test class for a simple lift door
 */
public class SimpleLiftDoorTest {

    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();
    private SimpleLiftDoor liftDoor;

    @AfterMethod
    public void tearDown() {
        liftDoor.shutdown();
    }

    @Test
    public void testDoorTimeout() throws InterruptedException {

        liftDoor = SimpleLiftDoor.newLiftDoor(new DoorStateListener(queue));
        liftDoor.fireEvent(SimpleLiftDoor.Cmd.OPEN);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS),
                SimpleLiftDoor.State.OPENED);
        Assert.assertEquals(queue.poll(1000, TimeUnit.MILLISECONDS),
                SimpleLiftDoor.State.CLOSED);
    }
}
