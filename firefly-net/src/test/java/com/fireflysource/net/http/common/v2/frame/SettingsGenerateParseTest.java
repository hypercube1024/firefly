package com.fireflysource.net.http.common.v2.frame;

import com.fireflysource.net.http.common.v2.decoder.Parser;
import com.fireflysource.net.http.common.v2.encoder.FrameBytes;
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator;
import com.fireflysource.net.http.common.v2.encoder.SettingsGenerator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsGenerateParseTest {

    @Test
    void testGenerateParseNoSettings() {
        List<SettingsFrame> frames = testGenerateParse(Collections.emptyMap());
        assertEquals(1, frames.size());
        SettingsFrame frame = frames.get(0);
        assertEquals(0, frame.getSettings().size());
        assertTrue(frame.isReply());
    }

    @Test
    void testGenerateParseSettings() {
        Map<Integer, Integer> settings1 = new HashMap<>();
        int key1 = 13;
        Integer value1 = 17;
        settings1.put(key1, value1);
        int key2 = 19;
        Integer value2 = 23;
        settings1.put(key2, value2);
        List<SettingsFrame> frames = testGenerateParse(settings1);
        assertEquals(1, frames.size());
        SettingsFrame frame = frames.get(0);
        Map<Integer, Integer> settings2 = frame.getSettings();
        assertEquals(2, settings2.size());
        assertEquals(value1, settings2.get(key1));
        assertEquals(value2, settings2.get(key2));
    }

    private List<SettingsFrame> testGenerateParse(Map<Integer, Integer> settings) {
        SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

        List<SettingsFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onSettings(SettingsFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateSettings(settings, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(buffer);
                }
            }
        }

        return frames;
    }

    @Test
    void testGenerateParseInvalidSettings() {
        SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

        AtomicInteger errorRef = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onConnectionFailure(int error, String reason) {
                errorRef.set(error);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        Map<Integer, Integer> settings1 = new HashMap<>();
        settings1.put(13, 17);
        FrameBytes frameBytes = generator.generateSettings(settings1, true);
        // Modify the length of the frame to make it invalid
        ByteBuffer bytes = frameBytes.getByteBuffers().get(0);
        bytes.putShort(1, (short) (bytes.getShort(1) - 1));

        for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
            while (buffer.hasRemaining()) {
                parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
            }
        }

        assertEquals(ErrorCode.FRAME_SIZE_ERROR.code, errorRef.get());
    }

    @Test
    void testGenerateParseOneByteAtATime() {
        SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

        List<SettingsFrame> frames = new ArrayList<>();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onSettings(SettingsFrame frame) {
                frames.add(frame);
            }
        }, 4096, 8192);
        parser.init(UnaryOperator.identity());

        Map<Integer, Integer> settings1 = new HashMap<>();
        int key = 13;
        Integer value = 17;
        settings1.put(key, value);

        // Iterate a few times to be sure generator and parser are properly reset.
        for (int i = 0; i < 2; ++i) {
            FrameBytes frameBytes = generator.generateSettings(settings1, true);

            frames.clear();
            for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
                while (buffer.hasRemaining()) {
                    parser.parse(ByteBuffer.wrap(new byte[]{buffer.get()}));
                }
            }

            assertEquals(1, frames.size());
            SettingsFrame frame = frames.get(0);
            Map<Integer, Integer> settings2 = frame.getSettings();
            assertEquals(1, settings2.size());
            assertEquals(value, settings2.get(key));
            assertTrue(frame.isReply());
        }
    }

    @Test
    void testGenerateParseTooManyDifferentSettingsInOneFrame() {
        SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

        AtomicInteger errorRef = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onConnectionFailure(int error, String reason) {
                errorRef.set(error);
            }
        }, 4096, 8192);
        int maxSettingsKeys = 32;
        parser.setMaxSettingsKeys(maxSettingsKeys);
        parser.init(UnaryOperator.identity());

        Map<Integer, Integer> settings = new HashMap<>();
        for (int i = 0; i < maxSettingsKeys + 1; ++i)
            settings.put(i + 10, i);

        FrameBytes frameBytes = generator.generateSettings(settings, false);

        for (ByteBuffer buffer : frameBytes.getByteBuffers()) {
            while (buffer.hasRemaining())
                parser.parse(buffer);
        }

        assertEquals(ErrorCode.ENHANCE_YOUR_CALM_ERROR.code, errorRef.get());
    }

    @Test
    void testGenerateParseTooManySameSettingsInOneFrame() throws Exception {
        int keyValueLength = 6;
        int pairs = Frame.DEFAULT_MAX_LENGTH / keyValueLength;
        int maxSettingsKeys = pairs / 2;

        AtomicInteger errorRef = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter(), 4096, 8192);
        parser.setMaxSettingsKeys(maxSettingsKeys);
        parser.setMaxFrameLength(Frame.DEFAULT_MAX_LENGTH);
        parser.init(listener -> new Parser.Listener.Wrapper(listener) {
            @Override
            public void onConnectionFailure(int error, String reason) {
                errorRef.set(error);
            }
        });

        int length = pairs * keyValueLength;
        ByteBuffer buffer = ByteBuffer.allocate(1 + 9 + length);
        buffer.putInt(length);
        buffer.put((byte) FrameType.SETTINGS.getType());
        buffer.put((byte) 0); // Flags.
        buffer.putInt(0); // Stream ID.
        // Add the same setting over and over again.
        for (int i = 0; i < pairs; ++i) {
            buffer.putShort((short) SettingsFrame.MAX_CONCURRENT_STREAMS);
            buffer.putInt(i);
        }
        // Only 3 bytes for the length, skip the first.
        buffer.flip().position(1);

        while (buffer.hasRemaining())
            parser.parse(buffer);

        assertEquals(ErrorCode.ENHANCE_YOUR_CALM_ERROR.code, errorRef.get());
    }

    @Test
    void testGenerateParseTooManySettingsInMultipleFrames() {
        SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

        AtomicInteger errorRef = new AtomicInteger();
        Parser parser = new Parser(new Parser.Listener.Adapter() {
            @Override
            public void onConnectionFailure(int error, String reason) {
                errorRef.set(error);
            }
        }, 4096, 8192);
        int maxSettingsKeys = 32;
        parser.setMaxSettingsKeys(maxSettingsKeys);
        parser.init(UnaryOperator.identity());

        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(13, 17);

        List<ByteBuffer> list = new LinkedList<>();
        for (int i = 0; i < maxSettingsKeys + 1; ++i) {
            FrameBytes frameBytes = generator.generateSettings(settings, false);
            list.addAll(frameBytes.getByteBuffers());
        }

        for (ByteBuffer buffer : list) {
            while (buffer.hasRemaining())
                parser.parse(buffer);
        }

        assertEquals(ErrorCode.ENHANCE_YOUR_CALM_ERROR.code, errorRef.get());
    }
}
