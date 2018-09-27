package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.PriorityGenerator;
import com.firefly.codec.http2.frame.PriorityFrame;

public class PriorityGenerateParseTest {

    @Test
    public void testGenerateParse() throws Exception {
        PriorityGenerator generator = new PriorityGenerator(new HeaderGenerator());

        final List<PriorityFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPriority(PriorityFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int parentStreamId = 17;
        int weight = 256;
        boolean exclusive = true;

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer buffer = generator.generatePriority(streamId, parentStreamId, weight, exclusive);

            frames.clear();
            while (buffer.hasRemaining()) {
                parser.parse(buffer);
            }

        }

        Assert.assertEquals(1, frames.size());
        PriorityFrame frame = frames.get(0);
        Assert.assertEquals(streamId, frame.getStreamId());
        Assert.assertEquals(parentStreamId, frame.getParentStreamId());
        Assert.assertEquals(weight, frame.getWeight());
        Assert.assertEquals(exclusive, frame.isExclusive());
    }

    @Test
    public void testGenerateParseOneByteAtATime() throws Exception {
        PriorityGenerator generator = new PriorityGenerator(new HeaderGenerator());

        final List<PriorityFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPriority(PriorityFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);

        int streamId = 13;
        int parentStreamId = 17;
        int weight = 3;
        boolean exclusive = true;

        // Iterate a few times to be sure generator and parser are properly
        // reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer buffer = generator.generatePriority(streamId, parentStreamId, weight, exclusive);

            frames.clear();
            while (buffer.hasRemaining()) {
                parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
            }

            Assert.assertEquals(1, frames.size());
            PriorityFrame frame = frames.get(0);
            Assert.assertEquals(streamId, frame.getStreamId());
            Assert.assertEquals(parentStreamId, frame.getParentStreamId());
            Assert.assertEquals(weight, frame.getWeight());
            Assert.assertEquals(exclusive, frame.isExclusive());
        }
    }
}
