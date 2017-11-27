package test.codec.websocket.encode;

import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.TextFrame;
import com.firefly.codec.websocket.frame.WebSocketFrame;
import com.firefly.codec.websocket.model.OpCode;
import com.firefly.codec.websocket.model.WebSocketPolicy;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;
import test.codec.websocket.IncomingFramesCapture;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;

public class GeneratorParserRoundtripTest {

    @Test
    public void testParserAndGenerator() throws Exception {
        WebSocketPolicy policy = WebSocketPolicy.newClientPolicy();
        Generator gen = new Generator(policy);
        Parser parser = new Parser(policy);
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        String message = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

        ByteBuffer out = BufferUtils.allocate(8192);
        // Generate Buffer
        BufferUtils.flipToFill(out);
        WebSocketFrame frame = new TextFrame().setPayload(message);
        ByteBuffer header = gen.generateHeaderBytes(frame);
        ByteBuffer payload = frame.getPayload();
        out.put(header);
        out.put(payload);

        // Parse Buffer
        BufferUtils.flipToFlush(out, 0);
        parser.parse(out);

        // Validate
        capture.assertNoErrors();
        capture.assertHasFrame(OpCode.TEXT, 1);

        TextFrame txt = (TextFrame) capture.getFrames().poll();
        Assert.assertThat("Text parsed", txt.getPayloadAsUTF8(), is(message));
    }

    @Test
    public void testParserAndGeneratorMasked() throws Exception {
        Generator gen = new Generator(WebSocketPolicy.newClientPolicy());
        Parser parser = new Parser(WebSocketPolicy.newServerPolicy());
        IncomingFramesCapture capture = new IncomingFramesCapture();
        parser.setIncomingFramesHandler(capture);

        String message = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

        ByteBuffer out = BufferUtils.allocate(8192);
        BufferUtils.flipToFill(out);
        // Setup Frame
        WebSocketFrame frame = new TextFrame().setPayload(message);

        // Add masking
        byte mask[] = new byte[4];
        Arrays.fill(mask, (byte) 0xFF);
        frame.setMask(mask);

        // Generate Buffer
        ByteBuffer header = gen.generateHeaderBytes(frame);
        ByteBuffer payload = frame.getPayload();
        out.put(header);
        out.put(payload);

        // Parse Buffer
        BufferUtils.flipToFlush(out, 0);
        parser.parse(out);

        // Validate
        capture.assertNoErrors();
        capture.assertHasFrame(OpCode.TEXT, 1);

        TextFrame txt = (TextFrame) capture.getFrames().poll();
        Assert.assertTrue("Text.isMasked", txt.isMasked());
        Assert.assertThat("Text parsed", txt.getPayloadAsUTF8(), is(message));
    }
}
