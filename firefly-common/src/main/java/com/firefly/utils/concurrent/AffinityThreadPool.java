package com.firefly.utils.concurrent;

import com.firefly.utils.exception.CommonRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class AffinityThreadPool extends AbstractExecutorService {

    private static final int defaultPoolSize = Runtime.getRuntime().availableProcessors();

    private final Thread[] threads;
    private final Work[] works;
    private final WorkSelector workSelector;
    private final int poolSize;
    private final CountDownLatch workCountDownLatch;

    public AffinityThreadPool() {
        this(new ThreadFactory() {

            AtomicInteger index = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "firefly-affinity-thread-" + index.getAndIncrement());
            }
        });
    }

    public AffinityThreadPool(ThreadFactory threadFactory) {
        this(defaultPoolSize, threadFactory, ((workSize, task) -> task.hashCode() % workSize));
    }

    public AffinityThreadPool(int poolSize, ThreadFactory threadFactory, WorkSelector workSelector) {
        this.poolSize = poolSize;
        this.workSelector = workSelector;
        threads = new Thread[poolSize];
        works = new Work[poolSize];
        workCountDownLatch = new CountDownLatch(poolSize);
        for (int i = 0; i < poolSize; i++) {
            works[i] = new Work(i, new LinkedTransferQueue<>());
            threads[i] = threadFactory.newThread(works[i]);
            threads[i].start();
        }
    }

    public interface WorkSelector {
        int select(int workSize, Object task);
    }

    private class Work implements Runnable {
        private final int id;
        private final BlockingQueue<Runnable> workQueue;
        private volatile boolean shutdownNow;
        private volatile boolean shutdown;
        private volatile boolean terminated;

        private Work(int id, BlockingQueue<Runnable> workQueue) {
            this.id = id;
            this.workQueue = workQueue;
        }

        @Override
        public void run() {
            while (!shutdownNow) {
                try {
                    Runnable r = workQueue.take();
                    r.run();
                    if (r instanceof ShutdownCommand) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            terminated = true;
            workCountDownLatch.countDown();
        }

        private void execute(Runnable command) {
            workQueue.offer(command);
        }

        private List<Runnable> shutdownNow() {
            shutdown = true;
            shutdownNow = true;
            try {
                threads[id].interrupt();
            } catch (Exception ignored) {

            }
            List<Runnable> tasks = new ArrayList<>();
            Runnable r;
            while ((r = workQueue.poll()) != null) {
                tasks.add(r);
            }
            return tasks;
        }

        private void shutdown() {
            try {
                shutdown = true;
                workQueue.put(new ShutdownCommand());
            } catch (InterruptedException e) {
                throw new CommonRuntimeException(e);
            }
        }

        private class ShutdownCommand implements Runnable {

            @Override
            public void run() {
                shutdownNow = true;
            }
        }

        private boolean isShutdown() {
            return shutdown;
        }

        private boolean isTerminated() {
            return terminated;
        }

        private int getId() {
            return id;
        }
    }

    @Override
    public void execute(Runnable command) {
        works[workSelector.select(poolSize, command)].execute(command);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable) {
            @Override
            public int hashCode() {
                return callable.hashCode();
            }
        };
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value) {
            @Override
            public int hashCode() {
                return runnable.hashCode();
            }
        };
    }

    @Override
    public void shutdown() {
        Arrays.stream(works).forEach(Work::shutdown);
    }

    @Override
    public List<Runnable> shutdownNow() {
        return Arrays.stream(works).flatMap(work -> work.shutdownNow().stream()).collect(Collectors.toList());
    }

    @Override
    public boolean isShutdown() {
        return Arrays.stream(works).allMatch(Work::isShutdown);
    }

    @Override
    public boolean isTerminated() {
        return Arrays.stream(works).allMatch(Work::isTerminated);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return workCountDownLatch.await(timeout, unit);
    }

}
