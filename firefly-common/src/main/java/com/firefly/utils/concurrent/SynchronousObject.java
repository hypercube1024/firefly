package com.firefly.utils.concurrent;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SynchronousObject<T> {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
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
