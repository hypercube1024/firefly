package com.firefly.utils.time;

public class TimeProvider {
	private final long interval;
	private volatile long current = System.currentTimeMillis();
	private volatile boolean start;

	public TimeProvider(long interval) {
		this.interval = interval;
	}

	public void start() {
		start = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (start) {
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					current = System.currentTimeMillis();
				}
			}
		}, "filefly time provider " + interval + "ms").start();
	}

	public void stop() {
		start = false;
	}

	public long currentTimeMillis() {
		return current;
	}

}
