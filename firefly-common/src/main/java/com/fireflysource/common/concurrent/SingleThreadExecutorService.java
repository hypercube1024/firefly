package com.fireflysource.common.concurrent;

import org.jctools.queues.MpscBlockingConsumerArrayQueue;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SingleThreadExecutorService extends AbstractExecutorService {

    private static final ThreadFactory defaultThreadFactory = r -> new Thread(r, "Firefly-MPSC-thread");
    private final Termination termination = new Termination();
    private final List<Runnable> notExecutedTasks = new LinkedList<>();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final MpscBlockingConsumerArrayQueue<Runnable> queue;
    private final Thread thread;

    public SingleThreadExecutorService(int capacity) {
        this(capacity, defaultThreadFactory);
    }

    public SingleThreadExecutorService(int capacity, ThreadFactory threadFactory) {
        this.queue = new MpscBlockingConsumerArrayQueue<>(capacity);
        thread = threadFactory.newThread(() -> {
            while (true) {
                if (executeTasks()) {
                    break;
                }
            }
        });
        thread.start();
    }

    private boolean executeTasks() {
        boolean exit;
        try {
            Runnable task = queue.take();
            if (task == termination) {
                exit = true;
            } else {
                task.run();
                exit = false;
            }
        } catch (InterruptedException e) {
            queue.drain(notExecutedTasks::add);
            exit = true;
        }
        return exit;
    }

    @Override
    public void shutdown() {
        if (queue.offer(termination)) {
            isShutdown.set(true);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        thread.interrupt();
        return notExecutedTasks;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return !thread.isAlive();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (isShutdown.get()) {
            thread.join(unit.toMillis(timeout));
        }
        return isTerminated();
    }

    @Override
    public void execute(Runnable command) {
        if (!queue.offer(command)) {
            throw new RejectedExecutionException();
        }
    }

    private static class Termination implements Runnable {
        @Override
        public void run() {

        }
    }
}
