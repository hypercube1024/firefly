package test.codec.websocket;

import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.OutgoingFrames;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

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
        Assert.assertThat("Capture index does not exist", idx, lessThan(captured.size()));
        ByteBuffer buf = captured.get(idx);
        String actualHex = TypeUtils.toHexString(BufferUtils.toArray(buf)).toUpperCase(Locale.ENGLISH);
        Assert.assertThat("captured[" + idx + "]", actualHex, is(expectedHex.toUpperCase(Locale.ENGLISH)));
    }

    public List<ByteBuffer> getCaptured() {
        return captured;
    }

    @Override
    public void outgoingFrame(Frame frame, Callback callback) {
        ByteBuffer buf = ByteBuffer.allocate(Generator.MAX_HEADER_LENGTH + frame.getPayloadLength());
        generator.generateWholeFrame(frame, buf);
        BufferUtils.flipToFlush(buf, 0);
        captured.add(buf);
        if (callback != null) {
            callback.succeeded();
        }
    }
}
