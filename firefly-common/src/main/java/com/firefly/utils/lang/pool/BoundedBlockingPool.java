package com.firefly.utils.lang.pool;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.firefly.utils.lang.AbstractLifeCycle;

public class BoundedBlockingPool<T> extends AbstractLifeCycle implements BlockingPool<T> {

	private BlockingQueue<T> queue;
	private int initSize;
	private ObjectFactory<T> factory;
	private Validator<T> validator;
	private Dispose<T> dispose;

	public BoundedBlockingPool(int initSize, int maxSize, ObjectFactory<T> factory, Validator<T> validator,
			Dispose<T> dispose) {
		this(initSize, new LinkedBlockingQueue<T>(maxSize), factory, validator, dispose);
	}

	public BoundedBlockingPool(int initSize, BlockingQueue<T> queue, ObjectFactory<T> factory, Validator<T> validator,
			Dispose<T> dispose) {
		this.initSize = initSize;
		this.factory = factory;
		this.validator = validator;
		this.dispose = dispose;
		this.queue = queue;
	}

	@Override
	public T get() {
		start();
		T t = queue.poll();
		return _take(t);
	}

	@Override
	public T take() throws InterruptedException {
		start();
		T t = queue.take();
		return _take(t);
	}

	@Override
	public T take(long time, TimeUnit unit) throws InterruptedException {
		start();
		T t = queue.poll(time, unit);
		return _take(t);
	}

	private T _take(T t) {
		if (t == null) {
			return factory.createNew();
		} else {
			if (validator.isValid(t)) {
				return t;
			} else {
				return factory.createNew();
			}
		}
	}

	@Override
	public void release(T t) {
		if (validator.isValid(t)) {
			boolean success = queue.offer(t);
			if (success == false) {
				dispose.destroy(t);
			}
		} else {
			dispose.destroy(t);
		}
	}

	@Override
	public void put(T t) throws InterruptedException {
		if (validator.isValid(t)) {
			queue.put(t);
		} else {
			dispose.destroy(t);
		}
	}

	@Override
	public boolean put(T t, long timeout, TimeUnit unit) throws InterruptedException {
		if (validator.isValid(t)) {
			boolean success = queue.offer(t, timeout, unit);
			if (success == false) {
				dispose.destroy(t);
			}
			return success;
		} else {
			dispose.destroy(t);
			return false;
		}
	}

	@Override
	protected void init() {

		for (int i = 0; i < initSize; i++) {
			queue.offer(factory.createNew());
		}
	}

	@Override
	protected void destroy() {
		T t = null;
		while ((t = queue.poll()) != null) {
			dispose.destroy(t);
		}
	}

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public void cleanup() {
		for (Iterator<T> iterator = queue.iterator(); iterator.hasNext();) {
			T t = iterator.next();
			if (validator.isValid(t) == false) {
				iterator.remove();
				dispose.destroy(t);
			}
		}
	}

}
