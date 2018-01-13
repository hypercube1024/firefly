package com.firefly.utils.lang;

import com.firefly.utils.function.Action0;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractLifeCycle implements LifeCycle {

    protected static final List<Action0> stopActions = new CopyOnWriteArrayList<>();

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

    protected volatile boolean start;

    public AbstractLifeCycle() {
        stopActions.add(this::stop);
    }

    @Override
    public boolean isStarted() {
        return start;
    }

    @Override
    public boolean isStopped() {
        return !start;
    }

    @Override
    public void start() {
        if (isStarted())
            return;

        synchronized (this) {
            if (isStarted())
                return;

            init();
            start = true;
        }
    }

    @Override
    public void stop() {
        if (isStopped())
            return;

        synchronized (this) {
            if (isStopped())
                return;

            destroy();
            start = false;
        }
    }

    abstract protected void init();

    abstract protected void destroy();

}
