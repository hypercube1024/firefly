package test.net.tcp;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

public class TestFilePartition {

    @Test
    public void test() {
        long len = 10;

        long bufferSize = 1024 * 8;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        Assert.assertThat(bufferCount, is(1L));
    }

    @Test
    public void test2() {
        long len = 1024 * 8;

        long bufferSize = 1024 * 8;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        Assert.assertThat(bufferCount, is(1L));
    }

    @Test
    public void test3() {
        long len = 1024 * 8 + 1;

        long bufferSize = 1024 * 8;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        Assert.assertThat(bufferCount, is(2L));
    }

    @Test
    public void test4() {
        long len = 1024 * 8 * 5;

        long bufferSize = 1024 * 8;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        Assert.assertThat(bufferCount, is(5L));
    }

    @Test
    public void test6() {
        long len = 1024 * 8 * 5 + 100;

        long bufferSize = 1024 * 8;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        Assert.assertThat(bufferCount, is(6L));
    }
}
