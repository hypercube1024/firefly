package com.fireflysource.net.websocket.extension.fragment;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.frame.*;
import com.fireflysource.net.websocket.model.*;
import com.fireflysource.net.websocket.utils.ByteBufferAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import static com.fireflysource.common.sys.Result.futureToConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


public class FragmentExtensionTest {

    /**
     * Verify that incoming frames are passed thru without modification
     */
    @Test
    public void testIncomingFrames() {
        IncomingFramesCapture capture = new IncomingFramesCapture();

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newClientPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment;maxLength=4");
        ext.setConfig(config);

        ext.setNextIncomingFrames(capture);

        // Quote
        List<String> quote = new ArrayList<>();
        quote.add("No amount of experimentation can ever prove me right;");
        quote.add("a single experiment can prove me wrong.");
        quote.add("-- Albert Einstein");

        // Manually create frame and pass into extension
        for (String q : quote) {
            Frame frame = new TextFrame().setPayload(q);
            ext.incomingFrame(frame);
        }

        int len = quote.size();
        capture.assertFrameCount(len);
        capture.assertHasFrame(OpCode.TEXT, len);

        String prefix;
        int i = 0;
        for (WebSocketFrame actual : capture.getFrames()) {
            prefix = "Frame[" + i + "]";

            assertEquals(OpCode.TEXT, actual.getOpCode());
            assertTrue(actual.isFin());
            assertFalse(actual.isRsv1());
            assertFalse(actual.isRsv2());
            assertFalse(actual.isRsv3());

            ByteBuffer expected = BufferUtils.toBuffer(quote.get(i), StandardCharsets.UTF_8);
            assertEquals(expected.remaining(), actual.getPayloadLength());
            ByteBufferAssert.assertEquals(prefix + ".payload", expected, actual.getPayload().slice());
            i++;
        }
    }

    /**
     * Incoming PING (Control Frame) should pass through extension unmodified
     */
    @Test
    public void testIncomingPing() {
        IncomingFramesCapture capture = new IncomingFramesCapture();

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment;maxLength=4");
        ext.setConfig(config);

        ext.setNextIncomingFrames(capture);

        String payload = "Are you there?";
        Frame ping = new PingFrame().setPayload(payload);
        ext.incomingFrame(ping);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.PING, 1);
        WebSocketFrame actual = capture.getFrames().poll();

        assertEquals(OpCode.PING, actual.getOpCode());
        assertTrue(actual.isFin());
        assertFalse(actual.isRsv1());
        assertFalse(actual.isRsv2());
        assertFalse(actual.isRsv3());

        ByteBuffer expected = BufferUtils.toBuffer(payload, StandardCharsets.UTF_8);
        assertEquals(expected.remaining(), actual.getPayloadLength());
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Verify that outgoing text frames are fragmented by the maxLength configuration.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingFramesByMaxLength() throws IOException, InterruptedException {
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment;maxLength=20");
        ext.setConfig(config);

        ext.setNextOutgoingFrames(capture);

        // Quote
        List<String> quote = new ArrayList<>();
        quote.add("No amount of experimentation can ever prove me right;");
        quote.add("a single experiment can prove me wrong.");
        quote.add("-- Albert Einstein");

        // Write quote as separate frames
        for (String section : quote) {
            Frame frame = new TextFrame().setPayload(section);
            ext.outgoingFrame(frame, null);
        }

        // Expected Frames
        List<WebSocketFrame> expectedFrames = new ArrayList<>();
        expectedFrames.add(new TextFrame().setPayload("No amount of experim").setFin(false));
        expectedFrames.add(new ContinuationFrame().setPayload("entation can ever pr").setFin(false));
        expectedFrames.add(new ContinuationFrame().setPayload("ove me right;").setFin(true));

        expectedFrames.add(new TextFrame().setPayload("a single experiment ").setFin(false));
        expectedFrames.add(new ContinuationFrame().setPayload("can prove me wrong.").setFin(true));

        expectedFrames.add(new TextFrame().setPayload("-- Albert Einstein").setFin(true));

        // capture.dump();

        int len = expectedFrames.size();
        capture.assertFrameCount(len);

        String prefix;
        LinkedBlockingDeque<WebSocketFrame> frames = capture.getFrames();
        for (int i = 0; i < len; i++) {
            prefix = "Frame[" + i + "]";
            WebSocketFrame actualFrame = frames.poll(1, SECONDS);
            WebSocketFrame expectedFrame = expectedFrames.get(i);

            // System.out.printf("actual: %s%n",actualFrame);
            // System.out.printf("expect: %s%n",expectedFrame);

            // Validate Frame
            assertEquals(expectedFrame.getOpCode(), actualFrame.getOpCode());
            assertEquals(expectedFrame.isFin(), actualFrame.isFin());
            assertEquals(expectedFrame.isRsv1(), actualFrame.isRsv1());
            assertEquals(expectedFrame.isRsv2(), actualFrame.isRsv2());
            assertEquals(expectedFrame.isRsv3(), actualFrame.isRsv3());

            // Validate Payload
            ByteBuffer expectedData = expectedFrame.getPayload().slice();
            ByteBuffer actualData = actualFrame.getPayload().slice();

            assertEquals(expectedData.remaining(), actualData.remaining());
            ByteBufferAssert.assertEquals(prefix + ".payload", expectedData, actualData);
        }
    }

    /**
     * Verify that outgoing text frames are not fragmented by default configuration (which has no maxLength specified)
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingFramesDefaultConfig() throws Exception {
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment");
        ext.setConfig(config);

        ext.setNextOutgoingFrames(capture);

        // Quote
        List<String> quote = new ArrayList<>();
        quote.add("No amount of experimentation can ever prove me right;");
        quote.add("a single experiment can prove me wrong.");
        quote.add("-- Albert Einstein");

        // Write quote as separate frames
        for (String section : quote) {
            Frame frame = new TextFrame().setPayload(section);
            ext.outgoingFrame(frame, null);
        }

        // Expected Frames
        List<WebSocketFrame> expectedFrames = new ArrayList<>();
        expectedFrames.add(new TextFrame().setPayload("No amount of experimentation can ever prove me right;"));
        expectedFrames.add(new TextFrame().setPayload("a single experiment can prove me wrong."));
        expectedFrames.add(new TextFrame().setPayload("-- Albert Einstein"));

        // capture.dump();

        int len = expectedFrames.size();
        capture.assertFrameCount(len);

        String prefix;
        LinkedBlockingDeque<WebSocketFrame> frames = capture.getFrames();
        for (int i = 0; i < len; i++) {
            prefix = "Frame[" + i + "]";
            WebSocketFrame actualFrame = frames.poll(1, SECONDS);
            WebSocketFrame expectedFrame = expectedFrames.get(i);

            // Validate Frame
            assertEquals(expectedFrame.getOpCode(), actualFrame.getOpCode());
            assertEquals(expectedFrame.isFin(), actualFrame.isFin());
            assertEquals(expectedFrame.isRsv1(), actualFrame.isRsv1());
            assertEquals(expectedFrame.isRsv2(), actualFrame.isRsv2());
            assertEquals(expectedFrame.isRsv3(), actualFrame.isRsv3());

            // Validate Payload
            ByteBuffer expectedData = expectedFrame.getPayload().slice();
            ByteBuffer actualData = actualFrame.getPayload().slice();

            assertEquals(expectedData.remaining(), actualData.remaining());
            ByteBufferAssert.assertEquals(prefix + ".payload", expectedData, actualData);
        }
    }

    /**
     * Outgoing PING (Control Frame) should pass through extension unmodified
     *
     * @throws IOException on test failure
     */
    @Test
    public void testOutgoingPing() throws IOException {
        OutgoingFramesCapture capture = new OutgoingFramesCapture();

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment;maxLength=4");
        ext.setConfig(config);

        ext.setNextOutgoingFrames(capture);

        String payload = "Are you there?";
        Frame ping = new PingFrame().setPayload(payload);

        ext.outgoingFrame(ping, null);

        capture.assertFrameCount(1);
        capture.assertHasFrame(OpCode.PING, 1);

        WebSocketFrame actual = capture.getFrames().getFirst();

        assertEquals(OpCode.PING, actual.getOpCode());
        assertTrue(actual.isFin());
        assertFalse(actual.isRsv1());
        assertFalse(actual.isRsv2());
        assertFalse(actual.isRsv3());

        ByteBuffer expected = BufferUtils.toBuffer(payload, StandardCharsets.UTF_8);
        assertEquals(expected.remaining(), actual.getPayloadLength());
        ByteBufferAssert.assertEquals("Frame.payload", expected, actual.getPayload().slice());
    }

    /**
     * Ensure that FragmentExtension honors the correct order of websocket frames.
     */
    @Test
    public void testLargeSmallTextAlternating() throws Exception {
        final int largeMessageSize = 60000;
        byte[] buf = new byte[largeMessageSize];
        Arrays.fill(buf, (byte) 'x');
        String largeMessage = new String(buf, UTF_8);

        final int fragmentCount = 10;
        final int fragmentLength = largeMessageSize / fragmentCount;
        final int messageCount = 10000;

        FragmentExtension ext = new FragmentExtension();
        ext.setPolicy(WebSocketPolicy.newServerPolicy());
        ExtensionConfig config = ExtensionConfig.parse("fragment;maxLength=" + fragmentLength);
        ext.setConfig(config);
        SaneFrameOrderingAssertion saneFrameOrderingAssertion = new SaneFrameOrderingAssertion();
        ext.setNextOutgoingFrames(saneFrameOrderingAssertion);

        CompletableFuture<Integer> enqueuedFrameCountFut = new CompletableFuture<>();

        CompletableFuture.runAsync(() ->
        {
            // Run Server Task
            int frameCount = 0;
            try {
                for (int i = 0; i < messageCount; i++) {
                    int messageId = i;
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    Consumer<Result<Void>> result = futureToConsumer(future);
                    WebSocketFrame frame;
                    if (i % 2 == 0) {
                        frame = new TextFrame().setPayload(largeMessage);
                        frameCount += fragmentCount;
                    } else {
                        frame = new TextFrame().setPayload("Short Message: " + i);
                        frameCount++;
                    }
                    ext.outgoingFrame(frame, result);
                    future.get();
                }
                enqueuedFrameCountFut.complete(frameCount);
            } catch (Throwable t) {
                enqueuedFrameCountFut.completeExceptionally(t);
            }
        });

        int enqueuedFrameCount = enqueuedFrameCountFut.get(5, SECONDS);

        int expectedFrameCount = (messageCount / 2) * fragmentCount; // large messages
        expectedFrameCount += (messageCount / 2); // + short messages

        assertEquals(expectedFrameCount, saneFrameOrderingAssertion.frameCount);
        assertEquals(expectedFrameCount, enqueuedFrameCount);
    }
}
