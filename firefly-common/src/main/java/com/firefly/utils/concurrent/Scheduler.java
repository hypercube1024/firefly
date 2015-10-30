package com.firefly.utils.concurrent;

import java.util.concurrent.TimeUnit;

public interface Scheduler {
	public interface Future {
		public boolean cancel();
	}
	
	public void shutdown();

	public Future schedule(Runnable task, long delay, TimeUnit unit);

	public Future scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit);

	public Future scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);
}
