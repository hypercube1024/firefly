package com.fireflysource.common.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Convenience auto closeable {@link ReentrantLock} wrapper.</p>
 *
 * <pre>
 * try (Locker.Lock lock = locker.lock())
 * {
 *   // something
 * }
 * </pre>
 */
public class Locker {
    private final ReentrantLock _lock = new ReentrantLock();
    private final Lock _unlock = new Lock();

    /**
     * <p>Acquires the lock.</p>
     *
     * @return the lock to unlock
     */
    public Lock lock() {
        _lock.lock();
        return _unlock;
    }

    /**
     * @return the lock to unlock
     * @deprecated use {@link #lock()} instead
     */
    @Deprecated
    public Lock lockIfNotHeld() {
        return lock();
    }

    /**
     * @return whether this lock has been acquired
     */
    public boolean isLocked() {
        return _lock.isLocked();
    }

    /**
     * @return a {@link Condition} associated with this lock
     */
    public Condition newCondition() {
        return _lock.newCondition();
    }

    /**
     * <p>The unlocker object that unlocks when it is closed.</p>
     */
    public class Lock implements AutoCloseable {
        @Override
        public void close() {
            _lock.unlock();
        }
    }

    @Deprecated
    public class UnLock extends Lock {
    }
}
