package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.GoAwayGenerator;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class GoAwayGenerateParseTest {

    @Test
    void testGenerateParse() {
        GoAwayGenerator generator = new GoAwayGenerator(new HeaderGenerator());

        final List<GoAwayFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onGoAway(GoAwayFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        int lastStreamId = 13;
        int error = 17;

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateGoAway(lastStreamId, error, null);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }
        }

        assertEquals(1, frames.size());
        GoAwayFrame frame = frames.get(0);
        assertEquals(lastStreamId, frame.getLastStreamId());
        assertEquals(error, frame.getError());
        assertNull(frame.getPayload());
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        GoAwayGenerator generator = new GoAwayGenerator(new HeaderGenerator());

        final List<GoAwayFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onGoAway(GoAwayFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        int lastStreamId = 13;
        int error = 17;
        byte[] payload = new byte[16];
        new Random().nextBytes(payload);

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateGoAway(lastStreamId, error, payload);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(1, frames.size());
            GoAwayFrame frame = frames.get(0);
            assertEquals(lastStreamId, frame.getLastStreamId());
            assertEquals(error, frame.getError());
            assertArrayEquals(payload, frame.getPayload());
        }
    }
}
