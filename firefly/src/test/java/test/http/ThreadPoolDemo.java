package test.http;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolDemo {

    public static class Task implements Runnable {

        public final long createTime = System.currentTimeMillis();
        private Object attachment;

        public Task(Object attachment) {
            this.attachment = attachment;
        }

        public Object getAttachment() {
            return attachment;
        }

        public void setAttachment(Object attachment) {
            this.attachment = attachment;
        }

        public long getCreateTime() {
            return createTime;
        }

        @Override
        public String toString() {
            return "Task [attachment=" + attachment + "]";
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("finish task: " + attachment);
        }

    }

    public static class MyFutureTask<T> extends FutureTask<T> {

        private Runnable currentRunnable;

        public MyFutureTask(Runnable runnable, T result) {
            super(runnable, result);
            currentRunnable = runnable;
        }

        public Runnable getCurrentRunnable() {
            return currentRunnable;
        }

    }

    public static void main(String[] args) {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(16);
        ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "firefly http handler thread");
            }
        };
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Task task = (Task) ((MyFutureTask<Void>) r).getCurrentRunnable();

                System.out.println("reject: " + task.toString() + "|" + (System.currentTimeMillis() - task.getCreateTime()) + "|" + Thread.currentThread().getName());
            }
        };
        ExecutorService excutor = new ThreadPoolExecutor(2, 4, 1, TimeUnit.SECONDS, workQueue, threadFactory, handler) {

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                return new MyFutureTask<T>(runnable, value);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                Task task = (Task) ((MyFutureTask<Void>) r).getCurrentRunnable();
                System.out.println("before execute: " + task.toString() + "|" + (System.currentTimeMillis() - task.getCreateTime()) + "|" + t.getName() + "|" + Thread.currentThread().getName());
                if (System.currentTimeMillis() - task.getCreateTime() > 5000)
                    ((MyFutureTask<Void>) r).cancel(false);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void afterExecute(Runnable r, Throwable t) {

                Task task = (Task) ((MyFutureTask<Void>) r).getCurrentRunnable();
//				System.out.println("after execute: " + task.toString()  + "|"+ (System.currentTimeMillis() - task.getCreateTime()));
                if (t != null) {
                    System.out.println("error occur: " + task.getAttachment());
                }
            }
        };
        for (int i = 0; i < 50; i++) {
            excutor.submit(new Task(i));
        }


    }

}
