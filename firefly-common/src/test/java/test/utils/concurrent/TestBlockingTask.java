package test.utils.concurrent;

import com.firefly.utils.concurrent.BlockingTask;

import java.util.stream.IntStream;

/**
 * @author Pengtao Qiu
 */
public class TestBlockingTask {
    public static void main(String[] args) {
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
            throw new Error(e);
        }
    }
}
