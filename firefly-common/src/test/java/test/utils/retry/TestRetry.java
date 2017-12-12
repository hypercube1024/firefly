package test.utils.retry;

import com.firefly.utils.retry.RetryTaskBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.firefly.utils.function.Predicates.of;
import static com.firefly.utils.retry.RetryStrategies.ifException;
import static com.firefly.utils.retry.RetryStrategies.ifResult;
import static com.firefly.utils.retry.StopStrategies.afterAttempt;
import static com.firefly.utils.retry.WaitStrategies.fixedWait;
import static java.util.function.Predicate.isEqual;

/**
 * @author Pengtao Qiu
 */
public class TestRetry {

    @Test
    public void test() {
        boolean success = RetryTaskBuilder.<Boolean>newTask()
                .retry(ifException(of(ex -> ex instanceof IOException).or(ex -> ex instanceof RuntimeException)))
                .retry(ifResult(isEqual(false)))
                .stop(afterAttempt(3))
                .wait(fixedWait(1, TimeUnit.SECONDS))
                .execute(() -> {
                    System.out.println("execute task");
                    return false;
                })
                .finish(ctx -> ctx.setResult(true))
                .call();
        Assert.assertTrue(success);
    }
}
