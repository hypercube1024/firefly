package test.codec.http2.frame;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.WindowUpdateGenerator;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WindowUpdateGenerateParseTest {

    @Test
    public void testGenerateParse() throws Exception {
        WindowUpdateGenerator generator = new WindowUpdateGenerator(new HeaderGenerator());

        final List<WindowUpdateFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onWindowUpdate(WindowUpdateFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int windowUpdate = 17;

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer buffer = generator.generateWindowUpdate(streamId, windowUpdate);

            frames.clear();
            while (buffer.hasRemaining()) {
                parser.parse(buffer);
            }

        }

        Assert.assertEquals(1, frames.size());
        WindowUpdateFrame frame = frames.get(0);
        Assert.assertEquals(streamId, frame.getStreamId());
        Assert.assertEquals(windowUpdate, frame.getWindowDelta());
    }

    @Test
    public void testGenerateParseOneByteAtATime() throws Exception {
        WindowUpdateGenerator generator = new WindowUpdateGenerator(new HeaderGenerator());

        final List<WindowUpdateFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onWindowUpdate(WindowUpdateFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int windowUpdate = 17;

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer buffer = generator.generateWindowUpdate(streamId, windowUpdate);

            frames.clear();
            while (buffer.hasRemaining()) {
                parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
            }

            Assert.assertEquals(1, frames.size());
            WindowUpdateFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertEquals(windowUpdate, frame.getWindowDelta());
        }
    }
}
