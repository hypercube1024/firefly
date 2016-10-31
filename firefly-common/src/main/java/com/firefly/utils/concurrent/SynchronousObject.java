package com.firefly.utils.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class SynchronousObject<T> {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private SynchronousQueue<T> queue = new SynchronousQueue<T>();
	
	public void put(T obj, long timeout) {
		try {
			queue.offer(obj, timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.error("put synchronous obj error", e);
		}
	}
	
	public T get(long timeout) {
		T t = null;
		try {
			t = queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.error("get synchronous obj error", e);
		}
		return t;
	}

}
