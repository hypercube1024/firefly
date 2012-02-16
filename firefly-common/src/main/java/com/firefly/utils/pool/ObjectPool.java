package com.firefly.utils.pool;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class ObjectPool {
	private Queue<Poolable> queue;
	private ObjectFactory objectFactory;
	
	public ObjectPool(ObjectFactory objectFactory) {
		queue = new ArrayBlockingQueue<Poolable>(16);
		this.objectFactory = objectFactory;
	}
	
	public ObjectPool(ObjectFactory objectFactory, int size) {
		queue = new ArrayBlockingQueue<Poolable>(size);
		this.objectFactory = objectFactory;
	}
	
	public Poolable get() {
		Poolable ret = queue.poll();
		if(ret == null) {
			ret = objectFactory.newInstance();
		} else {
			ret.prepare();
		}
		return ret;
	}
	
	public void put(Poolable poolable) {
		if(poolable != null) {
			poolable.release();
			queue.offer(poolable);
		}
	}
}
