package com.fireflysource.common.lifecycle;

public interface LifeCycle {
    void start();

    void stop();

    boolean isStarted();

    boolean isStopped();
}
