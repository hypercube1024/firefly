package com.firefly.utils.lang.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BoundedBlockingPool<T> extends AbstractPool<T> implements BlockingPool<T> {

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
		if (validator.isValid(t)) {
			return t;
		} else {
			return factory.createNew();
		}
	}

	@Override
	protected void handleInvalidReturn(T t) {
	}

	@Override
	protected void returnToPool(T t) {
		boolean success = queue.offer(t);
		if (success == false) {
			dispose.destroy(t);
		}
	}

	@Override
	protected boolean isValid(T t) {
		return validator.isValid(t);
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

}
