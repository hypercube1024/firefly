package com.firefly.utils.time;

import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.concurrent.ConcurrentLinkedQueue;

public class HashTimeWheel extends AbstractLifeCycle {

    private int maxTimers = 60; // slot's number in wheel
    private long interval = 1000; // the clock's accuracy

    private ConcurrentLinkedQueue<TimerTask>[] timerSlots;
    private volatile int currentSlot = 0;

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
     * @param delay after x milliseconds than execute runnable
     * @param run   run task in future
     * @return The task future
     */
    public Future add(long delay, Runnable run) {
        final int curSlot = currentSlot;

        final int ticks = delay > interval ? (int) (delay / interval) : 1; // figure out how many ticks need
        final int index = (curSlot + (ticks % maxTimers)) % maxTimers; // figure out the wheel's index
        final int round = (ticks - 1) / maxTimers; // the round number of spin

        TimerTask task = new TimerTask(round, run);
        timerSlots[index].add(task);

        return new Future(this, index, task);
    }

    private boolean remove(TimerTask task, int index) {
        return timerSlots[index].remove(task);
    }

    private final class Worker implements Runnable {

        @Override
        public void run() {
            while (start) {
                int currentSlotTemp = currentSlot;
                ConcurrentLinkedQueue<TimerTask> timerSlot = timerSlots[currentSlotTemp++];
                currentSlotTemp %= timerSlots.length;
                timerSlot.removeIf(TimerTask::runTask);
                ThreadUtils.sleep(interval);
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

    public static class Future implements Scheduler.Future {
        private HashTimeWheel timeWheel;
        private int index;
        private TimerTask task;

        public Future(HashTimeWheel timeWheel, int index, TimerTask task) {
            super();
            this.timeWheel = timeWheel;
            this.index = index;
            this.task = task;
        }

        /**
         * cancel current task
         *
         * @return if it return true, which cancel task success, else it fail
         */
        @Override
        public boolean cancel() {
            return timeWheel.remove(task, index);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void init() {
        timerSlots = new ConcurrentLinkedQueue[maxTimers];
        for (int i = 0; i < timerSlots.length; i++) {
            timerSlots[i] = new ConcurrentLinkedQueue<TimerTask>();
        }

        start = true;
        new Thread(new Worker(), "firefly time wheel").start();
    }

    @Override
    protected void destroy() {
        start = false;
        timerSlots = null;
    }

}
