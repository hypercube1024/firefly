package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import com.fireflysource.net.http.common.v2.encoder.ResetGenerator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResetGenerateParseTest {

    @Test
    void testGenerateParse() {
        ResetGenerator generator = new ResetGenerator(new HeaderGenerator());

        final List<ResetFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onReset(ResetFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        int streamId = 13;
        int error = 17;

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateReset(streamId, error);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }
        }

        assertEquals(1, frames.size());
        ResetFrame frame = frames.get(0);
        assertEquals(streamId, frame.getStreamId());
        assertEquals(error, frame.getError());
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        ResetGenerator generator = new ResetGenerator(new HeaderGenerator());

        final List<ResetFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onReset(ResetFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        int streamId = 13;
        int error = 17;

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateReset(streamId, error);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(1, frames.size());
            ResetFrame frame = frames.get(0);
            assertEquals(streamId, frame.getStreamId());
            assertEquals(error, frame.getError());
        }
    }
}
