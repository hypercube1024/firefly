package com.firefly.utils.concurrent;

import com.firefly.utils.exception.CommonRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class AffinityThreadPool implements ExecutorService {

    private static final int defaultPoolSize = Runtime.getRuntime().availableProcessors();

    private final Thread[] threads;
    private final Work[] works;
    private final WorkSelector workSelector;
    private final int poolSize;

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
        private volatile boolean shutdownNow = false;
        private volatile boolean shutdown;

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
            shutdown = true;
        }

        private void execute(Runnable command) {
            workQueue.offer(command);
        }

        private Future<?> submit(Runnable task) {
            Promise.Completable<Void> completable = new Promise.Completable<>();
            execute(() -> {
                try {
                    task.run();
                    completable.succeeded(null);
                } catch (Throwable t) {
                    completable.failed(t);
                }
            });
            return completable;
        }

        private <T> Future<T> submit(Callable<T> task) {
            Promise.Completable<T> completable = new Promise.Completable<>();
            execute(() -> {
                try {
                    completable.succeeded(task.call());
                } catch (Exception e) {
                    completable.failed(e);
                }
            });
            return completable;
        }

        private <T> Future<T> submit(Runnable task, T result) {
            Promise.Completable<T> completable = new Promise.Completable<>();
            execute(() -> {
                try {
                    task.run();
                    completable.succeeded(result);
                } catch (Exception e) {
                    completable.failed(e);
                }
            });
            return completable;
        }

        private List<Runnable> shutdownNow() {
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

        public boolean isShutdown() {
            return shutdown;
        }

        public int getId() {
            return id;
        }
    }

    @Override
    public void execute(Runnable command) {
        works[workSelector.select(poolSize, command)].execute(command);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return works[workSelector.select(poolSize, task)].submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return works[workSelector.select(poolSize, task)].submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return works[workSelector.select(poolSize, task)].submit(task, result);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return tasks.stream().map(this::submit).collect(Collectors.toList());
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
        throw new CommonRuntimeException("not implement method");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new CommonRuntimeException("not implement method");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new CommonRuntimeException("not implement method");
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new CommonRuntimeException("not implement method");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new CommonRuntimeException("not implement method");
    }

}
