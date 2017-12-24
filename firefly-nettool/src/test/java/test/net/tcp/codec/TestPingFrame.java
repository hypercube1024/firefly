package test.net.tcp.codec;

import com.firefly.net.tcp.codec.ffsocks.decode.FrameParser;
import com.firefly.net.tcp.codec.ffsocks.encode.FrameGenerator;
import com.firefly.net.tcp.codec.ffsocks.protocol.Frame;
import com.firefly.net.tcp.codec.ffsocks.protocol.FrameType;
import com.firefly.net.tcp.codec.ffsocks.protocol.PingFrame;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPingFrame {

    @Test
    public void test() {
        PingFrame pingFrame = new PingFrame(true);
        ByteBuffer buffer = FrameGenerator.generate(pingFrame);

        FrameParser parser = new FrameParser();
        parser.complete(frame -> {
            Assert.assertThat(frame.getType(), is(FrameType.PING));
            PingFrame p = (PingFrame) frame;
            Assert.assertTrue(p.isReply());
            System.out.println(p);
        }).exception(Throwable::printStackTrace);
        parser.receive(buffer);
    }

    @Test
    public void testSplit() {
        List<ByteBuffer> results = new ArrayList<>();

        int loop = 10;
        for (int i = 0; i < loop; i++) {
            PingFrame pingFrame = new PingFrame(i % 2 != 0);
            ByteBuffer buffer = FrameGenerator.generate(pingFrame);
            results.add(buffer);
        }

        List<ByteBuffer> splitBuffers = results.stream()
                                               .flatMap(buf -> BufferUtils.split(buf, 5).stream())
                                               .collect(Collectors.toList());
        List<Frame> messageFrames = new ArrayList<>();
        FrameParser parser = new FrameParser();
        parser.complete(messageFrames::add);
        splitBuffers.forEach(parser::receive);

        Assert.assertThat(messageFrames.size(), is(loop));
        for (int i = 0; i < loop; i++) {
            Frame frame = messageFrames.get(i);
            Assert.assertThat(frame.getType(), is(FrameType.PING));

            PingFrame p = (PingFrame) frame;
            Assert.assertThat(p.isReply(), is(i % 2 != 0));
            System.out.println(p);
        }
    }
}
