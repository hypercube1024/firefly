package com.firefly.utils.concurrent;

import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Func0;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Pengtao Qiu
 */
public class ReentrantSpinLocker {

    // use thread itself as  synchronization state
    private AtomicReference<Thread> owner = new AtomicReference<>();
    private int count = 0; // reentrant count of a thread, no need to be volatile

    public void lock() {
        Thread t = Thread.currentThread();
        if (t == owner.get()) { // if re-enter, increment the count.
            count++;
            return;
        }
        while (owner.compareAndSet(null, t)) {
        } //spin
    }

    public void unlock() {
        Thread t = Thread.currentThread();
        if (t == owner.get()) { //only the owner could do unlock;
            if (count > 0) {
                count--; // reentrant count not zero, just decrease the counter.
            } else {
                owner.set(null);// compareAndSet is not need here, already checked
            }
        }
    }

    public void lock(Action0 action0) {
        lock();
        try {
            action0.call();
            ;
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
