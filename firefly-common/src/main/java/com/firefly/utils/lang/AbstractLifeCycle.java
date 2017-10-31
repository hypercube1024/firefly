package com.firefly.utils.lang;

public abstract class AbstractLifeCycle implements LifeCycle {

    protected volatile boolean start;

    public AbstractLifeCycle() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "the firefly shutdown thread"));
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
