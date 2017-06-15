package com.firefly.utils.concurrent;

import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Func0;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Pengtao Qiu
 */
public class ReentrantLocker {

    private ReentrantLock lock = new ReentrantLock();

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void lock(Action0 action0) {
        lock();
        try {
            action0.call();;
        } finally {
            unlock();
        }
    }

    public <T> T lock(Func0<T> func0) {
        lock();
        try {
            return func0.call();
        } finally {
            unlock();
        }
    }
}
