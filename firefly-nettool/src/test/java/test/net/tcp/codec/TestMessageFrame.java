package test.net.tcp.codec;

import com.firefly.net.tcp.codec.decode.MessageFrameParser;
import com.firefly.net.tcp.codec.encode.MessageFrameGenerator;
import com.firefly.net.tcp.codec.protocol.ControlFrame;
import com.firefly.net.tcp.codec.protocol.FrameType;
import com.firefly.net.tcp.codec.protocol.MessageFrame;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestMessageFrame {

    @Test
    public void test() {
        MessageFrameGenerator generator = new MessageFrameGenerator();
        ControlFrame controlFrame = new ControlFrame(true, 1, true, "Hello".getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = generator.generate(controlFrame);

        MessageFrameParser parser = new MessageFrameParser();
        parser.complete(messageFrame -> {
            Assert.assertThat(messageFrame.getType(), is(FrameType.CONTROL));
            Assert.assertThat(messageFrame.getStreamId(), is(1));
            Assert.assertTrue(messageFrame.isEndStream());
            Assert.assertTrue(messageFrame.isEndFrame());

            String data = new String(messageFrame.getData(), StandardCharsets.UTF_8);
            Assert.assertThat(data, is("Hello"));
            System.out.println(messageFrame);
            System.out.println(data);
        }).exception(Throwable::printStackTrace);
        parser.receive(buffer);
    }

    @Test
    public void testSplit() {
        MessageFrameGenerator generator = new MessageFrameGenerator();
        List<ByteBuffer> results = new ArrayList<>();

        int loop = 10;
        for (int i = 0; i < loop; i++) {
            ControlFrame controlFrame = new ControlFrame(i == (loop - 1), 1, i == (loop - 1),
                    ("Hello_" + i).getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = generator.generate(controlFrame);
            results.add(buffer);
        }

        List<ByteBuffer> splitBuffers = results.stream()
                                               .flatMap(buf -> BufferUtils.split(buf, 7).stream())
                                               .collect(Collectors.toList());
        List<MessageFrame> messageFrames = new ArrayList<>();
        MessageFrameParser parser = new MessageFrameParser();
        parser.complete(messageFrames::add);
        splitBuffers.forEach(parser::receive);

        Assert.assertThat(messageFrames.size(), is(loop));
        for (int i = 0; i < loop; i++) {
            MessageFrame m = messageFrames.get(i);
            Assert.assertThat(m.getType(), is(FrameType.CONTROL));
            Assert.assertThat(m.getStreamId(), is(1));
            Assert.assertThat(m.isEndStream(), is(i == (loop - 1)));
            Assert.assertThat(m.isEndFrame(), is(i == (loop - 1)));

            String data = new String(m.getData(), StandardCharsets.UTF_8);
            Assert.assertThat(data, is("Hello_" + i));
            System.out.println(data);
        }
    }

}
