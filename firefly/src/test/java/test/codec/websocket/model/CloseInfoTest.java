package test.codec.websocket.model;

import com.firefly.codec.websocket.frame.CloseFrame;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.BufferUtils;
import org.junit.Test;

import java.nio.ByteBuffer;

import static com.firefly.codec.websocket.model.StatusCode.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class CloseInfoTest {
    /**
     * A test where no close is provided
     */
    @Test
    public void testAnonymousClose() {
        CloseInfo close = new CloseInfo();
        assertThat("close.code", close.getStatusCode(), is(NO_CODE));
        assertThat("close.reason", close.getReason(), nullValue());

        CloseFrame frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        // should result in no payload
        assertThat("close frame has payload", frame.hasPayload(), is(false));
        assertThat("close frame payload length", frame.getPayloadLength(), is(0));
    }

    /**
     * A test where NO_CODE (1005) is provided
     */
    @Test
    public void testNoCode() {
        CloseInfo close = new CloseInfo(NO_CODE);
        assertThat("close.code", close.getStatusCode(), is(NO_CODE));
        assertThat("close.reason", close.getReason(), nullValue());

        CloseFrame frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        // should result in no payload
        assertThat("close frame has payload", frame.hasPayload(), is(false));
        assertThat("close frame payload length", frame.getPayloadLength(), is(0));
    }

    /**
     * A test where NO_CLOSE (1006) is provided
     */
    @Test
    public void testNoClose() {
        CloseInfo close = new CloseInfo(NO_CLOSE);
        assertThat("close.code", close.getStatusCode(), is(NO_CLOSE));
        assertThat("close.reason", close.getReason(), nullValue());

        CloseFrame frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        // should result in no payload
        assertThat("close frame has payload", frame.hasPayload(), is(false));
        assertThat("close frame payload length", frame.getPayloadLength(), is(0));
    }

    /**
     * A test of FAILED_TLS_HANDSHAKE (1007)
     */
    @Test
    public void testFailedTlsHandshake() {
        CloseInfo close = new CloseInfo(FAILED_TLS_HANDSHAKE);
        assertThat("close.code", close.getStatusCode(), is(FAILED_TLS_HANDSHAKE));
        assertThat("close.reason", close.getReason(), nullValue());

        CloseFrame frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        // should result in no payload
        assertThat("close frame has payload", frame.hasPayload(), is(false));
        assertThat("close frame payload length", frame.getPayloadLength(), is(0));
    }

    /**
     * A test of NORMAL (1000)
     */
    @Test
    public void testNormal() {
        CloseInfo close = new CloseInfo(NORMAL);
        assertThat("close.code", close.getStatusCode(), is(NORMAL));
        assertThat("close.reason", close.getReason(), nullValue());

        CloseFrame frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        assertThat("close frame payload length", frame.getPayloadLength(), is(2));
    }

    private ByteBuffer asByteBuffer(int statusCode, String reason) {
        int len = 2; // status code length
        byte utf[] = null;
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
        assertThat("payload length", payload.remaining(), is(2));
        CloseFrame frame = new CloseFrame();
        frame.setPayload(payload);

        // create from frame
        CloseInfo close = new CloseInfo(frame);
        assertThat("close.code", close.getStatusCode(), is(NORMAL));
        assertThat("close.reason", close.getReason(), nullValue());

        // and back again
        frame = close.asFrame();
        assertThat("close frame op code", frame.getOpCode(), is(OpCode.CLOSE));
        assertThat("close frame payload length", frame.getPayloadLength(), is(2));
    }
}
