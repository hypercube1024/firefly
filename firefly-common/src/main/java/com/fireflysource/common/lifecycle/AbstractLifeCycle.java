package com.fireflysource.common.lifecycle;

import com.fireflysource.common.func.Callback;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifeCycle implements LifeCycle {

    private static final List<Callback> stopActions = new CopyOnWriteArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(AbstractLifeCycle::stopAll, "the firefly shutdown thread"));
    }

    protected AtomicBoolean start = new AtomicBoolean(false);
    private Callback stopCallback = this::stopNoRemove;

    public AbstractLifeCycle() {
        stopActions.add(stopCallback);
    }

    @Override
    public boolean isStarted() {
        return start.get();
    }

    @Override
    public boolean isStopped() {
        return !start.get();
    }

    @Override
    public void start() {
        if (start.compareAndSet(false, true)) {
            init();
        }
    }

    @Override
    public void stop() {
        if (start.compareAndSet(true, false)) {
            destroy();
            stopActions.remove(stopCallback);
        }
    }

    private void stopNoRemove() {
        if (start.compareAndSet(true, false)) {
            destroy();
        }
    }

    abstract protected void init();

    abstract protected void destroy();

    public static void stopAll() {
        if (!stopActions.isEmpty()) {
            stopActions.forEach(a -> {
                try {
                    a.call();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            });
            System.out.println("Shutdown instance: " + stopActions.size());
            stopActions.clear();
        }

        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if (iLoggerFactory instanceof Closeable) {
            try {
                ((Closeable) iLoggerFactory).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
