package test.utils.retry;

import com.firefly.utils.RandomUtils;
import com.firefly.utils.function.Predicates;
import com.firefly.utils.retry.RetryTaskBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.firefly.utils.function.Predicates.either;
import static com.firefly.utils.retry.RetryStrategies.ifException;
import static com.firefly.utils.retry.RetryStrategies.ifResult;
import static com.firefly.utils.retry.StopStrategies.*;
import static com.firefly.utils.retry.WaitStrategies.exponentialWait;
import static com.firefly.utils.retry.WaitStrategies.fixedWait;
import static java.util.function.Predicate.isEqual;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * @author Pengtao Qiu
 */
public class TestRetry {

    @Test
    public void test() {
        boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifException(either(ex -> ex instanceof IOException).or(ex -> ex instanceof RuntimeException)))
                .retry(ifResult(isEqual(false)))
                .stop(afterExecute(3))
                .wait(fixedWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("execute task");
                    return false;
                })
                .finish(ctx -> {
                    ctx.setResult(true);
                    long time = System.currentTimeMillis() - ctx.getStartTime();
                    Assert.assertThat(time, greaterThanOrEqualTo(10L));
                })
                .call();
        Assert.assertTrue(success);
    }

    @Test
    public void testTimeExceed() {
        boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifResult(isEqual(false)))
                .stop(afterDelay(40, TimeUnit.MILLISECONDS))
                .wait(fixedWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("execute task time exceed");
                    return false;
                })
                .finish(ctx -> {
                    ctx.setResult(true);
                    long time = System.currentTimeMillis() - ctx.getStartTime();
                    Assert.assertThat(time, greaterThanOrEqualTo(40L));
                })
                .call();
        Assert.assertTrue(success);
    }

    @Test
    public void testTimeExceedOrRetryCount() {
        boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifResult(isEqual(false)))
                .stop(afterDelay(200, TimeUnit.MILLISECONDS))
                .stop(afterExecute(2))
                .wait(fixedWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("execute task time exceed or executed count");
                    return false;
                })
                .finish(ctx -> {
                    ctx.setResult(true);
                    long time = System.currentTimeMillis() - ctx.getStartTime();
                    Assert.assertThat(time, greaterThanOrEqualTo(10L));
                })
                .call();
        Assert.assertTrue(success);
    }

    @Test
    public void testExponentialWait() {
        boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifResult(isEqual(false)))
                .stop(afterDelay(200, TimeUnit.MILLISECONDS))
                .wait(exponentialWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("execute task and exponential wait");
                    return false;
                })
                .finish(ctx -> {
                    ctx.setResult(true);
                    long time = System.currentTimeMillis() - ctx.getStartTime();
                    System.out.println(time);
                    Assert.assertThat(time, greaterThanOrEqualTo(10L));
                })
                .call();
        Assert.assertTrue(success);
    }

    @Test
    public void testNeverStop() {
        Integer ret = RetryTaskBuilder.<Integer>newTask()
                .retry(ifResult(Predicates.<Integer>of(v -> v >= 5).and(v -> v < 9)))
                .stop(never())
                .wait(fixedWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("never stop");
                    return (int) RandomUtils.random(1, 10);
                })
                .call();
        System.out.println(ret);
        Assert.assertTrue(ret < 5 || ret >= 9);
    }

    @Test
    public void testException() {
        Boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifException(ex -> ex instanceof RuntimeException))
                .stop(afterExecute(5))
                .wait(exponentialWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    System.out.println("execute task and exponential wait");
                    throw new RuntimeException("task exception");
                })
                .call();
        Assert.assertNull(success);
    }

}
