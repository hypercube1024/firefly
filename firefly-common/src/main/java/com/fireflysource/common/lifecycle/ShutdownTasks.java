package com.fireflysource.common.lifecycle;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShutdownTasks {

    private static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownTasks::stop, "Firefly-Shutdown-Tasks-Thread"));
    }

    public static void register(Runnable runnable) {
        tasks.add(runnable);
    }

    public static boolean remove(Runnable runnable) {
        return tasks.remove(runnable);
    }

    public static void stop() {
        while (true) {
            Runnable task = tasks.poll();
            if (task == null) {
                break;
            }

            try {
                task.run();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
