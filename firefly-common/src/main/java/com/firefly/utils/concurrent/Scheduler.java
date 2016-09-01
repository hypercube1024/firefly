package com.firefly.utils.concurrent;

import java.util.concurrent.TimeUnit;

import com.firefly.utils.lang.LifeCycle;

public interface Scheduler extends LifeCycle {
	
	public interface Future {
		public boolean cancel();
	}

	public Future schedule(Runnable task, long delay, TimeUnit unit);

	public Future scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit);

	public Future scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);
}
