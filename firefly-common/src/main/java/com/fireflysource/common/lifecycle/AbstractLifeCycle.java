package com.fireflysource.common.lifecycle;

import com.fireflysource.common.func.Callback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifeCycle implements LifeCycle {

    protected static final List<Callback> stopActions = new CopyOnWriteArrayList<>();

    static {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stopActions.forEach(a -> {
                    try {
                        a.call();
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                });
                System.out.println("Shutdown instance: " + stopActions.size());
                stopActions.clear();
            }, "the firefly shutdown thread"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    protected AtomicBoolean start = new AtomicBoolean(false);

    public AbstractLifeCycle() {
        stopActions.add(this::stop);
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
        }
    }

    abstract protected void init();

    abstract protected void destroy();

}
