package com.firefly.utils.time;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HashTimeWheel {
	private int maxTimers = 60; // wheel的格子数量
	private long interval = 1000; // wheel旋转时间间隔
	
	private ConcurrentLinkedQueue<TimerTask>[] timerSlots;
	private volatile int currentSlot = 0;
	private volatile boolean start;

	public int getMaxTimers() {
		return maxTimers;
	}

	public void setMaxTimers(int maxTimers) {
		this.maxTimers = maxTimers;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * add a task
	 * 
	 * @param delay
	 *           after x milliseconds than execute runnable
	 * @param run
	 *           run task in future
	 */
	public Future add(long delay, Runnable run) {
		final int curSlot = currentSlot;
		
		final int ticks = delay > interval ? (int) (delay / interval) : 1; // 计算刻度长度
		final int index = (curSlot + (ticks % maxTimers)) % maxTimers; // 放到wheel的位置
		final int round = (ticks - 1) / maxTimers; // wheel旋转的圈数

		TimerTask task = new TimerTask(round, run);
		timerSlots[index].add(task);
		
		return new Future(this, index, task);
	}
	
	private final boolean remove(TimerTask task, int index) {
		return timerSlots[index].remove(task);
	}

	@SuppressWarnings("unchecked")
	public void start() {
		if (!start) {
			synchronized (this) {
				if (!start) {
					timerSlots = new ConcurrentLinkedQueue[maxTimers];
					for (int i = 0; i < timerSlots.length; i++) {
						timerSlots[i] = new ConcurrentLinkedQueue<TimerTask>();
					}

					start = true;
					new Thread(new Worker(), "firefly time wheel").start();
				}
			}
		}
	}

	public void stop() {
		start = false;
		timerSlots = null;
	}

	private final class Worker implements Runnable {

		@Override
		public void run() {
			while (start) {
				int currentSlotTemp = currentSlot;
				ConcurrentLinkedQueue<TimerTask> timerSlot = timerSlots[currentSlotTemp++];
				currentSlotTemp %= timerSlots.length;

				for (Iterator<TimerTask> iterator = timerSlot.iterator(); iterator
						.hasNext();) {
					if (iterator.next().runTask())
						iterator.remove();
				}

				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				currentSlot = currentSlotTemp;
			}

		}

	}

	private final class TimerTask {
		private int round;
		private Runnable run;

		public TimerTask(int round, Runnable run) {
			this.round = round;
			this.run = run;
		}

		public boolean runTask() {
			if (round == 0) {
				run.run();
				return true;
			} else {
				round--;
				return false;
			}
		}
	}
	
	public static class Future {
		private HashTimeWheel timeWheel;
		private int index;
		private TimerTask task;
		
		public Future(HashTimeWheel timeWheel, int index, TimerTask task) {
			super();
			this.timeWheel = timeWheel;
			this.index = index;
			this.task = task;
		}



		public boolean cancel() {
			return timeWheel.remove(task, index);
		}
	}

}
