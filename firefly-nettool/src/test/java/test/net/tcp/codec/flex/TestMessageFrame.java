package test.net.tcp.codec.flex;

import com.firefly.net.tcp.codec.flex.decode.FrameParser;
import com.firefly.net.tcp.codec.flex.encode.FrameGenerator;
import com.firefly.net.tcp.codec.flex.protocol.*;
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
        ControlFrame controlFrame = new ControlFrame(true, 1, true, "Hello".getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = FrameGenerator.generate(controlFrame);

        FrameParser parser = new FrameParser();
        parser.complete(frame -> {
            Assert.assertThat(frame.getType(), is(FrameType.CONTROL));
            ControlFrame control = (ControlFrame) frame;

            Assert.assertThat(control.getStreamId(), is(1));
            Assert.assertTrue(control.isEndStream());
            Assert.assertTrue(control.isEndFrame());

            String data = new String(control.getData(), StandardCharsets.UTF_8);
            Assert.assertThat(data, is("Hello"));
            System.out.println(control);
            System.out.println(data);
        }).exception(Throwable::printStackTrace);
        parser.receive(buffer);
    }

    @Test
    public void testSplit() {
        List<ByteBuffer> results = new ArrayList<>();

        int loop = 10;
        for (int i = 0; i < loop; i++) {
            ControlFrame controlFrame = new ControlFrame(i == (loop - 1), 1, i == (loop - 1),
                    ("Hello_" + i).getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = FrameGenerator.generate(controlFrame);
            results.add(buffer);
        }

        List<ByteBuffer> splitBuffers = results.stream()
                                               .flatMap(buf -> BufferUtils.split(buf, 7).stream())
                                               .collect(Collectors.toList());
        List<Frame> messageFrames = new ArrayList<>();
        FrameParser parser = new FrameParser();
        parser.complete(messageFrames::add);
        splitBuffers.forEach(parser::receive);

        Assert.assertThat(messageFrames.size(), is(loop));
        for (int i = 0; i < loop; i++) {
            Frame frame = messageFrames.get(i);
            Assert.assertThat(frame.getType(), is(FrameType.CONTROL));

            MessageFrame m = (MessageFrame) frame;
            Assert.assertThat(m.getStreamId(), is(1));
            Assert.assertThat(m.isEndStream(), is(i == (loop - 1)));
            Assert.assertThat(m.isEndFrame(), is(i == (loop - 1)));

            String data = new String(m.getData(), StandardCharsets.UTF_8);
            Assert.assertThat(data, is("Hello_" + i));
            System.out.println(data);
        }
    }

    @Test
    public void testLengthIs0() {
        DataFrame dataFrame = new DataFrame(true, 11, true, null);
        ByteBuffer buffer = FrameGenerator.generate(dataFrame);

        FrameParser parser = new FrameParser();
        parser.complete(frame -> {
            Assert.assertThat(frame.getType(), is(FrameType.DATA));
            DataFrame data = (DataFrame) frame;

            Assert.assertThat(data.getStreamId(), is(11));
            Assert.assertTrue(data.isEndStream());
            Assert.assertTrue(data.isEndFrame());
            Assert.assertNull(data.getData());
            System.out.println(data);
        }).exception(Throwable::printStackTrace);
        parser.receive(buffer);
    }

}
