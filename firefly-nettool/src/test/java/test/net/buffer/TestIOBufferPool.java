package test.net.buffer;

import com.firefly.net.buffer.ThreadSafeIOBufferPool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestIOBufferPool {

    @Parameter
    public Run r;

    static class Run {
        ThreadSafeIOBufferPool bufferPool;
        boolean directBuffer;

        String testName;

        @Override
        public String toString() {
            return testName;
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> data = new ArrayList<>();
        Run run = new Run();
        run.bufferPool = new ThreadSafeIOBufferPool();
        run.directBuffer = true;
        run.testName = "test direct buffer pool";
        data.add(run);

        run = new Run();
        run.bufferPool = new ThreadSafeIOBufferPool(false);
        run.directBuffer = false;
        run.testName = "test heap buffer pool";
        data.add(run);
        return data;
    }

    @Test
    public void testDirectBuffer() {
        int max = 8;
        for (int i = 1; i <= max; i++) {
            ByteBuffer buffer = r.bufferPool.acquire(i * 1024);
            Assert.assertThat(r.bufferPool.size(), is(i - 1));
            System.out.println("buffer: " + buffer.remaining() + ", direct: " + buffer.isDirect());
            Assert.assertThat(buffer.isDirect(), is(r.directBuffer));
            r.bufferPool.release(buffer);
            Assert.assertThat(r.bufferPool.size(), is(i));

            ByteBuffer buffer2 = r.bufferPool.acquire(i * 1024);
            Assert.assertThat(r.bufferPool.size(), is(i - 1));
            Assert.assertThat(buffer2 == buffer, is(true));
            r.bufferPool.release(buffer2);
            Assert.assertThat(r.bufferPool.size(), is(i));
        }

        ByteBuffer buffer = r.bufferPool.acquire(16 * 1024);
        Assert.assertThat(r.bufferPool.size(), is(max));
        System.out.println("buffer: " + buffer.remaining() + ", direct: " + buffer.isDirect());
        Assert.assertThat(buffer.remaining(), is(16 * 1024));
        r.bufferPool.release(buffer);
        Assert.assertThat(r.bufferPool.size(), is(max));

        buffer = r.bufferPool.acquire(10 * 1024);
        Assert.assertThat(r.bufferPool.size(), is(max - 1));
        System.out.println("buffer: " + buffer.remaining() + ", direct: " + buffer.isDirect());
        Assert.assertThat(buffer.remaining(), is(16 * 1024));
        r.bufferPool.release(buffer);
        Assert.assertThat(r.bufferPool.size(), is(max));
    }
}
