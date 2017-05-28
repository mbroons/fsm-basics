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
public class StopWatchTest {

    private BlockingQueue<StateId> queue =
            new LinkedBlockingQueue<>();
    private StopWatch stopWatch;

    @AfterMethod
    public void tearDown() {
        stopWatch.shutdown();
    }

    @Test
    public void test_watch() throws InterruptedException {

        stopWatch = StopWatch.newStopWatch(new SimpleStateListener(queue));
        stopWatch.fireEvent(StopWatch.Cmd.START);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS),
                StopWatch.State.STARTED);

        TimeUnit.SECONDS.sleep(5);
        stopWatch.fireEvent(StopWatch.Cmd.STOP);
        Assert.assertEquals(queue.poll(100, TimeUnit.MILLISECONDS),
                StopWatch.State.STOPPED);
    }
}
