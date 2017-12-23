package test.net.tcp.codec;

import com.firefly.net.tcp.codec.protocol.Frame;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestEndFlagMask {

    @Test
    public void testShort() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) 33184).flip();
        System.out.println((short) 33184);

        short data = buffer.getShort();
        Assert.assertTrue(Frame.isEnd(data));

        int expectLength = 416;
        int length = Frame.removeEndFlag(data);
        Assert.assertThat(length, is(expectLength));

        short result = Frame.addEndFlag((short) expectLength);
        Assert.assertThat(result, is(data));

        int length2 = Frame.removeEndFlag((short) expectLength);
        Assert.assertThat(length2, is(expectLength));
    }

    @Test
    public void testInt() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(2098176).flip();

        int data = buffer.getInt();
        int result = Frame.addEndFlag(data);
        Assert.assertThat(result, is((int) 2149581824L));

        Assert.assertThat(Frame.removeEndFlag((int) 2149581824L), is(data));
        Assert.assertFalse(Frame.isEnd(data));
    }
}
