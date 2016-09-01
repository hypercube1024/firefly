package com.firefly.utils.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.firefly.utils.lang.AbstractLifeCycle;

abstract public class Schedulers {

	private Schedulers() {
	};

	private static ThreadFactory factory = new ThreadFactory() {

		@Override
		public Thread newThread(Runnable target) {
			return new Thread(target, "firefly scheduler");
		}
	};

	private static int coreSize = Runtime.getRuntime().availableProcessors();

	private static Scheduler wrapScheduledExecutorService(final ScheduledExecutorService service) {
		return new SchedulerService(service);
	}

	public static class SchedulerService extends AbstractLifeCycle implements Scheduler {

		private final ScheduledExecutorService service;

		public SchedulerService(ScheduledExecutorService service) {
			this.service = service;
		}

		@Override
		public Future schedule(Runnable task, long delay, TimeUnit unit) {
			start();
			return wrapScheduledFuture(service.schedule(task, delay, unit));
		}

		@Override
		public Future scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
			start();
			return wrapScheduledFuture(service.scheduleWithFixedDelay(task, initialDelay, delay, unit));
		}

		@Override
		public Future scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
			start();
			return wrapScheduledFuture(service.scheduleAtFixedRate(task, initialDelay, period, unit));
		}

		@Override
		protected void init() {
		}

		@Override
		protected void destroy() {
			service.shutdown();
		}

		private Future wrapScheduledFuture(final ScheduledFuture<?> future) {
			return () -> {
				return future.cancel(false);
			};
		}

	}

	public static Scheduler createScheduler(int corePoolSize) {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(corePoolSize, factory);
		executor.setRemoveOnCancelPolicy(true);
		return wrapScheduledExecutorService(executor);
	}

	public static Scheduler createScheduler() {
		return createScheduler(1);
	}

	public static Scheduler computation() {
		return createScheduler(coreSize);
	}
}
