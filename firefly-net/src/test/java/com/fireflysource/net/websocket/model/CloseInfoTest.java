package com.fireflysource.net.websocket.model;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.frame.CloseFrame;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static com.fireflysource.net.websocket.model.StatusCode.*;
import static org.junit.jupiter.api.Assertions.*;

public class CloseInfoTest {

    /**
     * A test where no close is provided
     */
    @Test
    public void testAnonymousClose() {
        CloseInfo close = new CloseInfo();
        assertEquals(NO_CODE, close.getStatusCode());
        assertNull(close.getReason());

        CloseFrame frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        // should result in no payload
        assertFalse(frame.hasPayload());
        assertEquals(0, frame.getPayloadLength());
    }

    /**
     * A test where NO_CODE (1005) is provided
     */
    @Test
    public void testNoCode() {
        CloseInfo close = new CloseInfo(NO_CODE);
        assertEquals(NO_CODE, close.getStatusCode());
        assertNull(close.getReason());

        CloseFrame frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        // should result in no payload
        assertFalse(frame.hasPayload());
        assertEquals(0, frame.getPayloadLength());
    }

    /**
     * A test where NO_CLOSE (1006) is provided
     */
    @Test
    public void testNoClose() {
        CloseInfo close = new CloseInfo(NO_CLOSE);
        assertEquals(NO_CLOSE, close.getStatusCode());
        assertNull(close.getReason());

        CloseFrame frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        // should result in no payload
        assertFalse(frame.hasPayload());
        assertEquals(0, frame.getPayloadLength());
    }

    /**
     * A test of FAILED_TLS_HANDSHAKE (1007)
     */
    @Test
    public void testFailedTlsHandshake() {
        CloseInfo close = new CloseInfo(FAILED_TLS_HANDSHAKE);
        assertEquals(FAILED_TLS_HANDSHAKE, close.getStatusCode());
        assertNull(close.getReason());

        CloseFrame frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        // should result in no payload
        assertFalse(frame.hasPayload());
        assertEquals(0, frame.getPayloadLength());
    }

    /**
     * A test of NORMAL (1000)
     */
    @Test
    public void testNormal() {
        CloseInfo close = new CloseInfo(NORMAL);
        assertEquals(NORMAL, close.getStatusCode());
        assertNull(close.getReason());

        CloseFrame frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        assertEquals(2, frame.getPayloadLength());
    }

    private ByteBuffer asByteBuffer(int statusCode, String reason) {
        int len = 2; // status code length
        byte[] utf = null;
        if (StringUtils.hasText(reason)) {
            utf = StringUtils.getUtf8Bytes(reason);
            len += utf.length;
        }

        ByteBuffer buf = BufferUtils.allocate(len);
        BufferUtils.flipToFill(buf);
        buf.put((byte) ((statusCode >>> 8) & 0xFF));
        buf.put((byte) ((statusCode >>> 0) & 0xFF));

        if (utf != null) {
            buf.put(utf, 0, utf.length);
        }
        BufferUtils.flipToFlush(buf, 0);

        return buf;
    }

    @Test
    public void testFromFrame() {
        ByteBuffer payload = asByteBuffer(NORMAL, null);
        assertEquals(2, payload.remaining());
        CloseFrame frame = new CloseFrame();
        frame.setPayload(payload);

        // create from frame
        CloseInfo close = new CloseInfo(frame);
        assertEquals(NORMAL, close.getStatusCode());
        assertNull(close.getReason());

        // and back again
        frame = close.asFrame();
        assertEquals(OpCode.CLOSE, frame.getOpCode());
        assertEquals(2, frame.getPayloadLength());
    }
}
