package com.firefly.utils.concurrent;

import java.util.concurrent.TimeUnit;

public interface Scheduler {
	public interface Future {
		public boolean cancel();
	}
	
	public Future schedule(Runnable task, long delay, TimeUnit units);
}
