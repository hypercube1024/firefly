package com.fireflysource.net.http.common.v2.frame;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.DataGenerator;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataGenerateParseTest {
    private final byte[] smallContent = new byte[128];
    private final byte[] largeContent = new byte[128 * 1024];

    DataGenerateParseTest() {
        Random random = new Random();
        random.nextBytes(smallContent);
        random.nextBytes(largeContent);
    }

    @Test
    void testGenerateParseNoContentNoPadding() {
        testGenerateParseContent(BufferUtils.EMPTY_BUFFER);
    }

    @Test
    void testGenerateParseSmallContentNoPadding() {
        testGenerateParseContent(ByteBuffer.wrap(smallContent));
    }

    private void testGenerateParseContent(ByteBuffer content) {
        List<DataFrame> frames = testGenerateParse(content);
        assertEquals(1, frames.size());
        DataFrame frame = frames.get(0);
        assertTrue(frame.getStreamId() != 0);
        assertTrue(frame.isEndStream());
        assertEquals(content, frame.getData());
    }

    @Test
    void testGenerateParseLargeContent() {
        ByteBuffer content = ByteBuffer.wrap(largeContent);
        List<DataFrame> frames = testGenerateParse(content);
        assertEquals(8, frames.size());
        ByteBuffer aggregate = ByteBuffer.allocate(content.remaining());
        for (int i = 1; i <= frames.size(); ++i) {
            DataFrame frame = frames.get(i - 1);
            assertTrue(frame.getStreamId() != 0);
            assertEquals(i == frames.size(), frame.isEndStream());
            aggregate.put(frame.getData());
        }
        aggregate.flip();
        assertEquals(content, aggregate);
    }

    private List<DataFrame> testGenerateParse(ByteBuffer data) {
        DataGenerator generator = new DataGenerator(new HeaderGenerator());

        final List<DataFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onData(DataFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer slice = data.slice();
            int generated = 0;
            List<ByteBuffer> list = new LinkedList<>();
            while (true) {
                FrameBytes frameBytes = generator.generateData(13, slice, true, slice.remaining());
                generated += frameBytes.getLength();
                generated -= Frame.HEADER_LENGTH;
                list.addAll(frameBytes.getByteBuffers());
                if (generated == data.remaining())
                    break;
            }

            frames.clear();
            for (ByteBuffer buffer : list) {
                parser.parse(buffer);
            }
        }

        return frames;
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        DataGenerator generator = new DataGenerator(new HeaderGenerator());

        final List<DataFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onData(DataFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            ByteBuffer data = ByteBuffer.wrap(largeContent);
            ByteBuffer slice = data.slice();
            int generated = 0;
            List<ByteBuffer> list = new LinkedList<>();
            while (true) {
                FrameBytes frameBytes = generator.generateData(13, slice, true, slice.remaining());
                generated += frameBytes.getLength();
                generated -= Frame.HEADER_LENGTH;
                list.addAll(frameBytes.getByteBuffers());
                if (generated == data.remaining())
                    break;
            }

            frames.clear();
            for (ByteBuffer buffer : list) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(largeContent.length, frames.size());
        }
    }
}
