package test.utils.heartbeat;

import com.firefly.utils.exception.CommonRuntimeException;
import com.firefly.utils.heartbeat.HealthCheck;
import com.firefly.utils.heartbeat.Result;
import com.firefly.utils.heartbeat.Task;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class TestHealthCheck {

    @Test
    public void test() {
        Phaser phaser = new Phaser(2);
        AtomicBoolean success = new AtomicBoolean(false);

        HealthCheck healthCheck = new HealthCheck(0, 1, TimeUnit.SECONDS);
        Task task = new Task();
        task.setName("test");
        task.setTask(() -> CompletableFuture.supplyAsync(() -> {
            System.out.println("heartbeat");
            return Result.SUCCESS;
        }));
        task.setResultListener((name, result, ex) -> {
            System.out.println(name + "|" + result);
            setResult(success, result);
            phaser.arrive();
        });
        healthCheck.register(task);
        healthCheck.start();

        phaser.arriveAndAwaitAdvance();
        Assert.assertTrue(success.get());
        healthCheck.stop();
    }

    @Test
    public void testException() {
        Phaser phaser = new Phaser(2);
        AtomicBoolean success = new AtomicBoolean(false);

        HealthCheck healthCheck = new HealthCheck(0, 1, TimeUnit.SECONDS);
        Task task = new Task();
        task.setName("test exception");
        task.setTask(() -> CompletableFuture.supplyAsync(() -> {
            System.out.println("exception");
            throw new CommonRuntimeException("heartbeat exception");
        }));
        task.setResultListener((name, result, ex) -> {
            System.out.println(name + "|" + result +
                    Optional.ofNullable(ex).map(Throwable::getMessage).map(s -> "|" + s).orElse(""));
            setResult(success, result);
            phaser.arrive();
        });
        healthCheck.register(task);
        healthCheck.start();

        phaser.arriveAndAwaitAdvance();
        Assert.assertFalse(success.get());
        healthCheck.stop();
    }

    private void setResult(AtomicBoolean success, Result result) {
        switch (result) {
            case SUCCESS:
                success.set(true);
                break;
            case FAILURE:
                success.set(false);
                break;
        }
    }
}
