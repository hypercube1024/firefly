package com.fireflysource.common.concurrent;

import com.fireflysource.common.sys.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class IteratingCallbackTest {
    private ScheduledExecutorService scheduler;

    @BeforeEach
    public void prepare() {
        scheduler = Executors.newScheduledThreadPool(4);
    }

    @AfterEach
    public void dispose() {
        ExecutorServiceUtils.shutdownAndAwaitTermination(scheduler, 1, TimeUnit.SECONDS);
    }

    @Test
    public void testNonWaitingProcess() throws Exception {
        TestCB cb = new TestCB() {
            int i = 10;

            @Override
            protected Action process() {
                processed++;
                if (i-- > 1) {
                    accept(Result.SUCCESS); // fake a completed IO operation
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };

        cb.iterate();
        assertTrue(cb.waitForComplete());
        assertEquals(10, cb.processed);
    }

    @Test
    public void testWaitingProcess() throws Exception {
        TestCB cb = new TestCB() {
            int i = 4;

            @Override
            protected Action process() throws Exception {
                processed++;
                if (i-- > 1) {
                    scheduler.schedule(successTask, 50, TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };

        cb.iterate();

        assertTrue(cb.waitForComplete());

        assertEquals(4, cb.processed);
    }

    @Test
    public void testWaitingProcessSpuriousIterate() throws Exception {
        final TestCB cb = new TestCB() {
            int i = 4;

            @Override
            protected Action process() throws Exception {
                processed++;
                if (i-- > 1) {
                    scheduler.schedule(successTask, 50, TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };

        cb.iterate();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                cb.iterate();
                if (!cb.isSucceeded())
                    scheduler.schedule(this, 50, TimeUnit.MILLISECONDS);
            }
        }, 49, TimeUnit.MILLISECONDS);

        assertTrue(cb.waitForComplete());

        assertEquals(4, cb.processed);
    }

    @Test
    public void testNonWaitingProcessFailure() throws Exception {
        TestCB cb = new TestCB() {
            int i = 10;

            @Override
            protected Action process() {
                processed++;
                if (i-- > 1) {
                    if (i > 5)
                        accept(Result.SUCCESS); // fake a completed IO operation
                    else
                        accept(Result.createFailedResult(new Exception("testing")));
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };

        cb.iterate();
        assertFalse(cb.waitForComplete());
        assertEquals(5, cb.processed);
    }

    @Test
    public void testWaitingProcessFailure() throws Exception {
        TestCB cb = new TestCB() {
            int i = 4;

            @Override
            protected Action process() {
                processed++;
                if (i-- > 1) {
                    scheduler.schedule(i > 2 ? successTask : failTask, 50, TimeUnit.MILLISECONDS);
                    return Action.SCHEDULED;
                }
                return Action.SUCCEEDED;
            }
        };

        cb.iterate();

        assertFalse(cb.waitForComplete());
        assertEquals(2, cb.processed);
    }

    @Test
    public void testIdleWaiting() throws Exception {
        final CountDownLatch idle = new CountDownLatch(1);

        TestCB cb = new TestCB() {
            int i = 5;

            @Override
            protected Action process() {
                processed++;

                switch (i--) {
                    case 5:
                        accept(Result.SUCCESS);
                        return Action.SCHEDULED;

                    case 4:
                        scheduler.schedule(successTask, 5, TimeUnit.MILLISECONDS);
                        return Action.SCHEDULED;

                    case 3:
                        scheduler.schedule(() -> idle.countDown(), 5, TimeUnit.MILLISECONDS);
                        return Action.IDLE;

                    case 2:
                        accept(Result.SUCCESS);
                        return Action.SCHEDULED;

                    case 1:
                        scheduler.schedule(successTask, 5, TimeUnit.MILLISECONDS);
                        return Action.SCHEDULED;

                    case 0:
                        return Action.SUCCEEDED;

                    default:
                        throw new IllegalStateException();
                }
            }
        };

        cb.iterate();
        idle.await(10, TimeUnit.SECONDS);
        assertTrue(cb.isIdle());

        cb.iterate();
        assertTrue(cb.waitForComplete());
        assertEquals(6, cb.processed);
    }

    @Test
    public void testCloseDuringProcessingReturningScheduled() throws Exception {
        testCloseDuringProcessing(IteratingCallback.Action.SCHEDULED);
    }

    @Test
    public void testCloseDuringProcessingReturningSucceeded() throws Exception {
        testCloseDuringProcessing(IteratingCallback.Action.SUCCEEDED);
    }

    private void testCloseDuringProcessing(final IteratingCallback.Action action) throws Exception {
        final CountDownLatch failureLatch = new CountDownLatch(1);
        IteratingCallback callback = new IteratingCallback() {
            @Override
            protected Action process() throws Exception {
                close();
                return action;
            }

            @Override
            protected void onCompleteFailure(Throwable cause) {
                failureLatch.countDown();
            }
        };

        callback.iterate();

        assertTrue(failureLatch.await(5, TimeUnit.SECONDS));
    }

    private abstract static class TestCB extends IteratingCallback {
        protected Runnable successTask = () -> accept(Result.SUCCESS);
        protected Runnable failTask = () -> accept(Result.createFailedResult(new Exception("testing failure")));
        protected CountDownLatch completed = new CountDownLatch(1);
        protected int processed = 0;

        @Override
        protected void onCompleteSuccess() {
            completed.countDown();
        }

        @Override
        public void onCompleteFailure(Throwable x) {
            completed.countDown();
        }

        boolean waitForComplete() throws InterruptedException {
            completed.await(10, TimeUnit.SECONDS);
            return isSucceeded();
        }
    }
}
