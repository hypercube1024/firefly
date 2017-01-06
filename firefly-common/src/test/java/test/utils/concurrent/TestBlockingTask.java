package test.utils.concurrent;

import com.firefly.utils.concurrent.BlockingTask;
import com.firefly.utils.exception.CommonRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;

/**
 * @author Pengtao Qiu
 */
public class TestBlockingTask {
    public static void main(String[] args) {
        List<ForkJoinTask<Integer>> tasks = new ArrayList<>();
        ForkJoinPool pool = new ForkJoinPool(2);
        for (int i = 0; i < 10; i++) {
            final int x = i;
            ForkJoinTask<Integer> task = pool.submit(() -> BlockingTask.callInManagedBlock(() -> {
                sleep();
                return x;
            }));
            tasks.add(task);
        }
        tasks.forEach(task -> {
            try {
                System.out.println(task.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main2(String[] args) {
        long start = System.currentTimeMillis();
        System.out.println("start");
        IntStream.range(0, 10).parallel().forEach((x) -> sleep());
        long end = System.currentTimeMillis();
        System.out.println("end -> " + (end - start));

        start = System.currentTimeMillis();
        System.out.println("start2");
        IntStream.range(0, 10).parallel().forEach((x) -> BlockingTask.callInManagedBlock(() -> {
            sleep();
            return x;
        }));
        end = System.currentTimeMillis();
        System.out.println("end2 -> " + (end - start));
    }

    public static void sleep() {
        try {
            System.out.println(System.currentTimeMillis() + ": Sleeping " + Thread.currentThread().getName());
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new CommonRuntimeException(e);
        }
    }
}
