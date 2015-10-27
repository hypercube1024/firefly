package com.firefly.utils.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * This is a lock designed to protect VERY short sections of critical code.
 * Threads attempting to take the lock will wait until the lock is available,
 * thus it is important that the code protected by this lock is extremely simple
 * and non blocking.
 * </p>
 * 
 * <pre>
 * try (SpinLock.Lock lock = locker.lock()) {
 * 	// something very quick and non blocking
 * }
 * </pre>
 */
public class Locker {
	private static final boolean SPIN = Boolean.getBoolean(Locker.class.getName() + ".spin");

	private final boolean _spin;
	private final ReentrantLock _lock = new ReentrantLock();
	private final AtomicReference<Thread> _spinLockState = new AtomicReference<>(null);
	private final Lock _unlock = new Lock();

	public Locker() {
		this(SPIN);
	}

	public Locker(boolean spin) {
		this._spin = spin;
	}

	public Lock lock() {
		if (_spin)
			spinLock();
		else
			concLock();
		return _unlock;
	}

	private void spinLock() {
		Thread current = Thread.currentThread();
		while (true) {
			// Using test-and-test-and-set for better performance.
			Thread locker = _spinLockState.get();
			if (locker != null || !_spinLockState.compareAndSet(null, current)) {
				if (locker == current)
					throw new IllegalStateException("Locker is not reentrant");
				continue;
			}
			return;
		}
	}

	private void concLock() {
		if (_lock.isHeldByCurrentThread())
			throw new IllegalStateException("Locker is not reentrant");
		_lock.lock();
	}

	public boolean isLocked() {
		if (_spin)
			return _spinLockState.get() != null;
		else
			return _lock.isLocked();
	}

	public class Lock implements AutoCloseable {
		@Override
		public void close() {
			if (_spin)
				_spinLockState.set(null);
			else
				_lock.unlock();
		}
	}
}
