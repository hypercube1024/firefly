package com.firefly.net.support;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class Synchronizer<T> {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private SynchronousObject<T>[] objs;
	private final long timeout;
	private int size;

	public Synchronizer() {
		this(0, 0);
	}

	public Synchronizer(int size, long timeout) {
		if (size <= 0) {
			this.size = 1024 * 4;
		} else {
			int i = 2;
			while (i < size)
				i <<= 1;

			this.size = i;
		}
		log.info("synchronizer size: {}", this.size);
		this.timeout = timeout > 0 ? timeout : 5000;
		init();
		log.debug("client timeout {}", timeout);
	}

	public T get(int index) {
		log.debug("get index {}", index);
		return objs[index & (size - 1)].get(timeout);
	}

	public void put(int index, T t) {
		log.debug("put index {}", index);
		objs[index & (size - 1)].put(t, timeout);
	}

	@SuppressWarnings("unchecked")
	public void init() {
		objs = new SynchronousObject[size];

		for (int i = 0; i < objs.length; i++) {
			objs[i] = new SynchronousObject<T>();
		}

	}

}
