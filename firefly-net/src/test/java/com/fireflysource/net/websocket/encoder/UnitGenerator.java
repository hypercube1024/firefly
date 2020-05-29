package com.fireflysource.net.websocket.encoder;


import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.websocket.frame.Frame;
import com.fireflysource.net.websocket.frame.WebSocketFrame;
import com.fireflysource.net.websocket.model.WebSocketPolicy;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Convenience Generator.
 */
public class UnitGenerator extends Generator {

    private static final LazyLogger LOG = SystemLogger.create(UnitGenerator.class);

    public static ByteBuffer generate(Frame frame) {
        return generate(new Frame[]{frame});
    }

    /**
     * Generate All Frames into a single ByteBuffer.
     * <p>
     * This is highly inefficient and is not used in production! (This exists to make testing of the Generator easier)
     *
     * @param frames the frames to generate from
     * @return the ByteBuffer representing all of the generated frames provided.
     */
    public static ByteBuffer generate(Frame[] frames) {
        Generator generator = new UnitGenerator();

        // Generate into single bytebuffer
        int buflen = 0;
        for (Frame f : frames) {
            buflen += f.getPayloadLength() + Generator.MAX_HEADER_LENGTH;
        }
        ByteBuffer completeBuf = ByteBuffer.allocate(buflen);
        BufferUtils.clearToFill(completeBuf);

        // Generate frames
        for (Frame f : frames) {
            generator.generateWholeFrame(f, completeBuf);
        }

        BufferUtils.flipToFlush(completeBuf, 0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("generate({} frames) - {}", frames.length, BufferUtils.toDetailString(completeBuf));
        }
        return completeBuf;
    }

    /**
     * Generate a single giant buffer of all provided frames Not appropriate for production code, but useful for testing.
     *
     * @param frames the list of frames to generate from
     * @return the bytebuffer representing all of the generated frames
     */
    public static ByteBuffer generate(List<WebSocketFrame> frames) {
        // Create non-symmetrical mask (helps show mask bytes order issues)
        final byte[] MASK =
                {0x11, 0x22, 0x33, 0x44};

        // the generator
        Generator generator = new UnitGenerator();

        // Generate into single bytebuffer
        int buflen = 0;
        for (Frame f : frames) {
            buflen += f.getPayloadLength() + Generator.MAX_HEADER_LENGTH;
        }
        ByteBuffer completeBuf = ByteBuffer.allocate(buflen);
        BufferUtils.clearToFill(completeBuf);

        // Generate frames
        for (WebSocketFrame f : frames) {
            f.setMask(MASK); // make sure we have the test mask set
            BufferUtils.put(generator.generateHeaderBytes(f), completeBuf);
            ByteBuffer window = f.getPayload();
            if (BufferUtils.hasContent(window)) {
                BufferUtils.put(window, completeBuf);
            }
        }

        BufferUtils.flipToFlush(completeBuf, 0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("generate({} frames) - {}", frames.size(), BufferUtils.toDetailString(completeBuf));
        }
        return completeBuf;
    }

    public UnitGenerator() {
        super(WebSocketPolicy.newServerPolicy());
    }
}
