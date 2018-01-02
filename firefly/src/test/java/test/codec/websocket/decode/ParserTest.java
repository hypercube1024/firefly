package test.codec.websocket.decode;


import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.exception.ProtocolException;
import com.firefly.codec.websocket.frame.*;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.StatusCode;
import com.firefly.codec.websocket.model.WebSocketBehavior;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import test.codec.websocket.utils.Hex;
import com.firefly.utils.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import test.codec.websocket.IncomingFramesCapture;
import test.codec.websocket.UnitGenerator;
import test.codec.websocket.UnitParser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ParserTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Similar to the server side 5.15 testcase. A normal 2 fragment text text message, followed by another continuation.
     */
    @Test
    public void testParseCase5_15() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("fragment1").setFin(false));
        send.add(new ContinuationFrame().setPayload("fragment2").setFin(true));
        send.add(new ContinuationFrame().setPayload("fragment3").setFin(false)); // bad frame
        send.add(new TextFrame().setPayload("fragment4").setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        expectedException.expect(ProtocolException.class);
        expectedException.expectMessage(containsString("CONTINUATION frame without prior !FIN"));
        parser.parseQuietly(completeBuf);
    }

    /**
     * Similar to the server side 5.18 testcase. Text message fragmented as 2 frames, both as opcode=TEXT
     */
    @Test
    public void testParseCase5_18() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("fragment1").setFin(false));
        send.add(new TextFrame().setPayload("fragment2").setFin(true)); // bad frame, must be continuation
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        expectedException.expect(ProtocolException.class);
        expectedException.expectMessage(containsString("Unexpected TEXT frame"));
        parser.parseQuietly(completeBuf);
    }

    /**
     * Similar to the server side 5.19 testcase. text message, send in 5 frames/fragments, with 2 pings in the mix.
     */
    @Test
    public void testParseCase5_19() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new TextFrame().setPayload("f1").setFin(false));
        send.add(new ContinuationFrame().setPayload(",f2").setFin(false));
        send.add(new PingFrame().setPayload("pong-1"));
        send.add(new ContinuationFrame().setPayload(",f3").setFin(false));
        send.add(new ContinuationFrame().setPayload(",f4").setFin(false));
        send.add(new PingFrame().setPayload("pong-2"));
        send.add(new ContinuationFrame().setPayload(",f5").setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parseQuietly(completeBuf);

        capture.assertErrorCount(0);
        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CONTINUATION, 4);
        capture.assertHasFrame(OpCode.CLOSE, 1);
        capture.assertHasFrame(OpCode.PING, 2);
    }

    /**
     * Similar to the server side 5.6 testcase. pong, then text, then close frames.
     */
    @Test
    public void testParseCase5_6() {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new PongFrame().setPayload("ping"));
        send.add(new TextFrame().setPayload("hello, world"));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(completeBuf);

        capture.assertErrorCount(0);
        capture.assertHasFrame(OpCode.TEXT, 1);
        capture.assertHasFrame(OpCode.CLOSE, 1);
        capture.assertHasFrame(OpCode.PONG, 1);
    }

    /**
     * Similar to the server side 6.2.3 testcase. Lots of small 1 byte UTF8 Text frames, representing 1 overall text message.
     */
    @Test
    public void testParseCase6_2_3() {
        String utf8 = "Hello-\uC2B5@\uC39F\uC3A4\uC3BC\uC3A0\uC3A1-UTF-8!!";
        byte msg[] = StringUtils.getUtf8Bytes(utf8);

        List<WebSocketFrame> send = new ArrayList<>();
        int textCount = 0;
        int continuationCount = 0;
        int len = msg.length;
        boolean continuation = false;
        byte mini[];
        for (int i = 0; i < len; i++) {
            DataFrame frame = null;
            if (continuation) {
                frame = new ContinuationFrame();
                continuationCount++;
            } else {
                frame = new TextFrame();
                textCount++;
            }
            mini = new byte[1];
            mini[0] = msg[i];
            frame.setPayload(ByteBuffer.wrap(mini));
            boolean isLast = (i >= (len - 1));
            frame.setFin(isLast);
            send.add(frame);
            continuation = true;
        }
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        ByteBuffer completeBuf = UnitGenerator.generate(send);
        UnitParser parser = new UnitParser();
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(completeBuf);

        capture.assertErrorCount(0);
        capture.assertHasFrame(OpCode.TEXT, textCount);
        capture.assertHasFrame(OpCode.CONTINUATION, continuationCount);
        capture.assertHasFrame(OpCode.CLOSE, 1);
    }

    @Test
    public void testParseNothing() {
        ByteBuffer buf = ByteBuffer.allocate(16);
        // Put nothing in the buffer.
        buf.flip();

        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);
        parser.parse(buf);

        capture.assertNoErrors();
        Assert.assertThat("Frame Count", capture.getFrames().size(), is(0));
    }

    @Test
    public void testWindowedParseLargeFrame() {
        // Create frames
        byte payload[] = new byte[65536];
        Arrays.fill(payload, (byte) '*');

        List<WebSocketFrame> frames = new ArrayList<>();
        TextFrame text = new TextFrame();
        text.setPayload(ByteBuffer.wrap(payload));
        text.setMask(Hex.asByteArray("11223344"));
        frames.add(text);
        frames.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        // Build up raw (network bytes) buffer
        ByteBuffer networkBytes = UnitGenerator.generate(frames);

        // Parse, in 4096 sized windows
        WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        Parser parser = new UnitParser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        while (networkBytes.remaining() > 0) {
            ByteBuffer window = networkBytes.slice();
            int windowSize = Math.min(window.remaining(), 4096);
            window.limit(windowSize);
            parser.parse(window);
            networkBytes.position(networkBytes.position() + windowSize);
        }

        capture.assertNoErrors();
        Assert.assertThat("Frame Count", capture.getFrames().size(), is(2));
        WebSocketFrame frame = capture.getFrames().poll();
        Assert.assertThat("Frame[0].opcode", frame.getOpCode(), is(OpCode.TEXT));
        ByteBuffer actualPayload = frame.getPayload();
        Assert.assertThat("Frame[0].payload.length", actualPayload.remaining(), is(payload.length));
        // Should be all '*' characters (if masking is correct)
        for (int i = actualPayload.position(); i < actualPayload.remaining(); i++) {
            Assert.assertThat("Frame[0].payload[i]", actualPayload.get(i), is((byte) '*'));
        }
    }
}
