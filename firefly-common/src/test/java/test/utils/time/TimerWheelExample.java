package test.utils.time;

import com.firefly.utils.time.HashTimeWheel;
import com.firefly.utils.time.TimeProvider;

public class TimerWheelExample {
    public void test() {
        final HashTimeWheel t = new HashTimeWheel();
        t.setMaxTimers(5);
        t.setInterval(100);
        t.start();

        try {
            Thread.sleep(130L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        t.add(400, new Runnable() {
            private long start = System.currentTimeMillis();

            @Override
            public void run() {
                long end = System.currentTimeMillis();
                System.out.println("t1: " + (end - start));
            }
        });

        t.add(900, new Runnable() {
            private long start = System.currentTimeMillis();

            @Override
            public void run() {
                long end = System.currentTimeMillis();
                System.out.println("t1: " + (end - start));
            }
        });

        t.add(2500, new Runnable() {
            private long start = System.currentTimeMillis();

            @Override
            public void run() {
                long end = System.currentTimeMillis();
                System.out.println("t2: " + (end - start));
                t.add(1200, new Runnable() {
                    @Override
                    public void run() {
                        long end = System.currentTimeMillis();
                        System.out.println("t2: " + (end - start));
                    }
                });
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        new TimerWheelExample().test();
        TimeProvider t = new TimeProvider(1000L);
        t.start();

        Thread.sleep(1000L);
        long start = t.currentTimeMillis();
        Thread.sleep(5000L);
        System.out.println("TimeProvider: " + (t.currentTimeMillis() - start));
    }

    public static void main1(String[] args) {
        final HashTimeWheel t = new HashTimeWheel();
        t.setMaxTimers(5);
        t.setInterval(100);
        t.start();

        HashTimeWheel.Future future = t.add(3000, new Runnable() {

            @Override
            public void run() {
                System.out.println("run !!");
            }
        });
        t.add(3000, new Runnable() {

            @Override
            public void run() {
                System.out.println("run2 !!");
            }
        });
        future.cancel();

    }
}
