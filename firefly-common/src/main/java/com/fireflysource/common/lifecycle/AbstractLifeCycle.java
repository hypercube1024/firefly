package com.fireflysource.common.lifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifeCycle implements LifeCycle {

    protected AtomicBoolean start = new AtomicBoolean(false);

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
            try {
                destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    abstract protected void init();

    abstract protected void destroy();

}
