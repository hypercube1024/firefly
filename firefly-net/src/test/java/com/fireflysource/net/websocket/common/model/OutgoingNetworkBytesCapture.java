package com.fireflysource.net.websocket.common.model;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.common.sys.Result;
import com.fireflysource.net.websocket.common.encoder.Generator;
import com.fireflysource.net.websocket.common.frame.Frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Capture outgoing network bytes.
 */
public class OutgoingNetworkBytesCapture implements OutgoingFrames {
    private final Generator generator;
    private List<ByteBuffer> captured;

    public OutgoingNetworkBytesCapture(Generator generator) {
        this.generator = generator;
        this.captured = new ArrayList<>();
    }

    public void assertBytes(int idx, String expectedHex) {
        assertTrue(idx < captured.size());
        ByteBuffer buf = captured.get(idx);
        String actualHex = TypeUtils.toHexString(BufferUtils.toArray(buf)).toUpperCase(Locale.ENGLISH);
        assertEquals(expectedHex.toUpperCase(Locale.ENGLISH), actualHex);
    }

    public List<ByteBuffer> getCaptured() {
        return captured;
    }

    @Override
    public void outgoingFrame(Frame frame, Consumer<Result<Void>> result) {
        ByteBuffer buf = ByteBuffer.allocate(Generator.MAX_HEADER_LENGTH + frame.getPayloadLength());
        generator.generateWholeFrame(frame, buf);
        BufferUtils.flipToFlush(buf, 0);
        captured.add(buf);
        if (result != null) {
            result.accept(Result.SUCCESS);
        }
    }
}
