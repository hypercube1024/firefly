package com.fireflysource.net.websocket.common.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.common.decoder.Parser;
import com.fireflysource.net.websocket.common.decoder.UnitParser;
import com.fireflysource.net.websocket.common.frame.*;
import com.fireflysource.net.websocket.common.model.*;
import com.fireflysource.net.websocket.common.utils.Hex;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class GeneratorTest {
    private static final LazyLogger LOG = SystemLogger.create(WindowHelper.class);

    public static class WindowHelper {
        final int windowSize;
        int totalParts;
        int totalBytes;

        public WindowHelper(int windowSize) {
            this.windowSize = windowSize;
            this.totalParts = 0;
            this.totalBytes = 0;
        }

        public ByteBuffer generateWindowed(Frame... frames) {
            // Create Buffer to hold all generated frames in a single buffer
            int completeBufSize = 0;
            for (Frame f : frames) {
                completeBufSize += Generator.MAX_HEADER_LENGTH + f.getPayloadLength();
            }

            ByteBuffer completeBuf = ByteBuffer.allocate(completeBufSize);
            BufferUtils.clearToFill(completeBuf);

            // Generate from all frames
            Generator generator = new UnitGenerator();

            for (Frame f : frames) {
                ByteBuffer header = generator.generateHeaderBytes(f);
                totalBytes += BufferUtils.put(header, completeBuf);

                if (f.hasPayload()) {
                    ByteBuffer payload = f.getPayload();
                    totalBytes += payload.remaining();
                    totalParts++;
                    completeBuf.put(payload.slice());
                }
            }

            // Return results
            BufferUtils.flipToFlush(completeBuf, 0);
            return completeBuf;
        }

        public void assertTotalParts(int expectedParts) {
            assertEquals(expectedParts, totalParts);
        }

        public void assertTotalBytes(int expectedBytes) {
            assertEquals(expectedBytes, totalBytes);
        }
    }

    private void assertGeneratedBytes(CharSequence expectedBytes, Frame... frames) {
        // collect up all frames as single ByteBuffer
        ByteBuffer allframes = UnitGenerator.generate(frames);
        // Get hex String form of all frames bytebuffer.
        String actual = Hex.asHex(allframes);
        // Validate
        assertEquals(expectedBytes.toString(), actual);
    }

    private String asMaskedHex(String str, byte[] maskingKey) {
        byte[] utf = StringUtils.getUtf8Bytes(str);
        mask(utf, maskingKey);
        return Hex.asHex(utf);
    }

    private void mask(byte[] buf, byte[] maskingKey) {
        int size = buf.length;
        for (int i = 0; i < size; i++) {
            buf[i] ^= maskingKey[i % 4];
        }
    }

    @Test
    public void testCloseEmpty() {
        // 0 byte payload (no status code)
        assertGeneratedBytes("8800", new CloseFrame());
    }

    @Test
    public void testCloseCodeNoReason() {
        CloseInfo close = new CloseInfo(StatusCode.NORMAL);
        // 2 byte payload (2 bytes for status code)
        assertGeneratedBytes("880203E8", close.asFrame());
    }

    @Test
    public void testCloseCodeOkReason() {
        CloseInfo close = new CloseInfo(StatusCode.NORMAL, "OK");
        // 4 byte payload (2 bytes for status code, 2 more for "OK")
        assertGeneratedBytes("880403E84F4B", close.asFrame());
    }

    @Test
    public void testTextHello() {
        WebSocketFrame frame = new TextFrame().setPayload("Hello");
        byte[] utf = StringUtils.getUtf8Bytes("Hello");
        assertGeneratedBytes("8105" + Hex.asHex(utf), frame);
    }

    @Test
    public void testTextMasked() {
        WebSocketFrame frame = new TextFrame().setPayload("Hello");
        byte[] maskingKey = Hex.asByteArray("11223344");
        frame.setMask(maskingKey);

        // what is expected
        StringBuilder expected = new StringBuilder();
        expected.append("8185").append("11223344");
        expected.append(asMaskedHex("Hello", maskingKey));

        // validate
        assertGeneratedBytes(expected, frame);
    }

    @Test
    public void testTextMaskedOffsetSourceByteBuffer() {
        ByteBuffer payload = ByteBuffer.allocate(100);
        payload.position(5);
        payload.put(StringUtils.getUtf8Bytes("Hello"));
        payload.flip();
        payload.position(5);
        // at this point, we have a ByteBuffer of 100 bytes.
        // but only a few bytes in the middle are made available for the payload.
        // we are testing that masking works as intended, even if the provided
        // payload does not start at position 0.
        LOG.debug("Payload = {}", BufferUtils.toDetailString(payload));
        WebSocketFrame frame = new TextFrame().setPayload(payload);
        byte[] maskingKey = Hex.asByteArray("11223344");
        frame.setMask(maskingKey);

        // what is expected
        StringBuilder expected = new StringBuilder();
        expected.append("8185").append("11223344");
        expected.append(asMaskedHex("Hello", maskingKey));

        // validate
        assertGeneratedBytes(expected, frame);
    }

    /**
     * Prevent regression of masking of many packets.
     */
    @Test
    public void testManyMasked() {
        int pingCount = 2;

        // Prepare frames
        WebSocketFrame[] frames = new WebSocketFrame[pingCount + 1];
        for (int i = 0; i < pingCount; i++) {
            frames[i] = new PingFrame().setPayload(String.format("ping-%d", i));
        }
        frames[pingCount] = new CloseInfo(StatusCode.NORMAL).asFrame();

        // Mask All Frames
        byte[] maskingKey = Hex.asByteArray("11223344");
        for (WebSocketFrame f : frames) {
            f.setMask(maskingKey);
        }

        // Validate result of generation
        StringBuilder expected = new StringBuilder();
        expected.append("8986").append("11223344");
        expected.append(asMaskedHex("ping-0", maskingKey)); // ping 0
        expected.append("8986").append("11223344");
        expected.append(asMaskedHex("ping-1", maskingKey)); // ping 1
        expected.append("8882").append("11223344");
        byte[] closure = Hex.asByteArray("03E8");
        mask(closure, maskingKey);
        expected.append(Hex.asHex(closure)); // normal closure

        assertGeneratedBytes(expected, frames);
    }

    /**
     * Test the windowed generate of a frame that has no masking.
     */
    @Test
    public void testWindowedGenerate() {
        // A decent sized frame, no masking
        byte[] payload = new byte[10240];
        Arrays.fill(payload, (byte) 0x44);

        WebSocketFrame frame = new BinaryFrame().setPayload(payload);

        // Generate
        int windowSize = 1024;
        WindowHelper helper = new WindowHelper(windowSize);
        ByteBuffer completeBuffer = helper.generateWindowed(frame);

        // Validate
        int expectedHeaderSize = 4;
        int expectedSize = payload.length + expectedHeaderSize;
        int expectedParts = 1;

        helper.assertTotalParts(expectedParts);
        helper.assertTotalBytes(payload.length + expectedHeaderSize);

        assertEquals(expectedSize, completeBuffer.remaining());
    }

    @Test
    public void testWindowedGenerateWithMasking() {
        // A decent sized frame, with masking
        byte[] payload = new byte[10240];
        Arrays.fill(payload, (byte) 0x55);

        byte[] mask = new byte[]
                {0x2A, (byte) 0xF0, 0x0F, 0x00};

        WebSocketFrame frame = new BinaryFrame().setPayload(payload);
        frame.setMask(mask); // masking!

        // Generate
        int windowSize = 2929;
        WindowHelper helper = new WindowHelper(windowSize);
        ByteBuffer completeBuffer = helper.generateWindowed(frame);

        // Validate
        int expectedHeaderSize = 8;
        int expectedSize = payload.length + expectedHeaderSize;
        int expectedParts = 1;

        helper.assertTotalParts(expectedParts);
        helper.assertTotalBytes(payload.length + expectedHeaderSize);

        assertEquals(expectedSize, completeBuffer.remaining());

        // Parse complete buffer.
        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        parser.parse(completeBuffer);

        // Assert validity of frame
        WebSocketFrame actual = capture.getFrames().poll();
        assertEquals(OpCode.BINARY, actual.getOpCode());
        assertEquals(payload.length, actual.getPayloadLength());

        // Validate payload contents for proper masking
        ByteBuffer actualData = actual.getPayload().slice();
        assertEquals(payload.length, actualData.remaining());
        while (actualData.remaining() > 0) {
            assertEquals((byte) 0x55, actualData.get());
        }
    }
}
