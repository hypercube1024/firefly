package test.utils.codec;

import com.firefly.utils.codec.ByteArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;

public class TestByteArrayUtils {
    @Test
    public void testSplitData() {
        byte[] data = "hello world!".getBytes();
        List<byte[]> list = ByteArrayUtils.splitData(data, 5);
        Assert.assertThat(list.size(), is(3));
        Assert.assertThat(new String(list.get(0)), is("hello"));
        Assert.assertThat(new String(list.get(1)), is(" worl"));
        Assert.assertThat(new String(list.get(2)), is("d!"));

        list = ByteArrayUtils.splitData(data, 6);
        Assert.assertThat(list.size(), is(2));

        list = ByteArrayUtils.splitData(data, 12);
        Assert.assertThat(list.size(), is(1));

        list = ByteArrayUtils.splitData(data, 5, 2);
        Assert.assertThat(list.size(), is(2));
        Assert.assertThat(new String(list.get(0)), is("hello"));
        Assert.assertThat(new String(list.get(1)), is(" world!"));

        list = ByteArrayUtils.splitData(data, 2, 3);
        Assert.assertThat(list.size(), is(3));
        Assert.assertThat(new String(list.get(0)), is("he"));
        Assert.assertThat(new String(list.get(1)), is("ll"));
        Assert.assertThat(new String(list.get(2)), is("o world!"));

        list = ByteArrayUtils.splitData(data, 12, 3);
        Assert.assertThat(list.size(), is(1));
        Assert.assertThat(new String(list.get(0)), is("hello world!"));

        list = ByteArrayUtils.splitData(data, 7, 4);
        Assert.assertThat(list.size(), is(2));
        Assert.assertThat(new String(list.get(0)), is("hello w"));
        Assert.assertThat(new String(list.get(1)), is("orld!"));
    }
}
