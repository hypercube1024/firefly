package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import com.fireflysource.net.http.common.v2.encoder.PingGenerator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class PingGenerateParseTest {

    @Test
    void testGenerateParse() {
        PingGenerator generator = new PingGenerator(new HeaderGenerator());

        final List<PingFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPing(PingFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        byte[] payload = new byte[8];
        new Random().nextBytes(payload);

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generatePing(payload, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }
        }

        assertEquals(1, frames.size());
        PingFrame frame = frames.get(0);
        assertArrayEquals(payload, frame.getPayload());
        assertTrue(frame.isReply());
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        PingGenerator generator = new PingGenerator(new HeaderGenerator());

        final List<PingFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPing(PingFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        byte[] payload = new byte[8];
        new Random().nextBytes(payload);

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generatePing(payload, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(1, frames.size());
            PingFrame frame = frames.get(0);
            assertArrayEquals(payload, frame.getPayload());
            assertTrue(frame.isReply());
        }
    }

    @Test
    void testPayloadAsLong() {
        PingGenerator generator = new PingGenerator(new HeaderGenerator());

        final List<PingFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onPing(PingFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        PingFrame ping = new PingFrame(System.nanoTime(), true);
        FrameBytes frameBytes = generator.generate(ping);

        for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
            while (buffer.hasRemaining()) {
                parser.parse(buffer);
            }
        }

        assertEquals(1, frames.size());
        PingFrame pong = frames.get(0);
        assertEquals(ping.getPayloadAsLong(), pong.getPayloadAsLong());
        assertTrue(pong.isReply());
    }
}
