package com.firefly.utils.concurrent;

import com.firefly.utils.function.Func0;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Pengtao Qiu
 */
public class SimpleLock {

    private final ReentrantLock lock = new ReentrantLock();

    public <R> R lock(Func0<R> func0) {
        try {
            lock.lock();
            return func0.call();
        } finally {
            lock.unlock();
        }
    }
}
