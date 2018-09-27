package com.firefly.codec.http2.encode;

import java.nio.ByteBuffer;
import java.util.List;

import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.Frame;
import com.firefly.codec.http2.frame.FrameType;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.utils.lang.Pair;

public class Generator {
    private final HeaderGenerator headerGenerator;
    private final HpackEncoder hpackEncoder;
    private final FrameGenerator[] generators;
    private final DataGenerator dataGenerator;

    public Generator() {
        this(4096, 0);
    }

    public Generator(int maxDynamicTableSize, int maxHeaderBlockFragment) {

        headerGenerator = new HeaderGenerator();
        hpackEncoder = new HpackEncoder(maxDynamicTableSize);

        this.generators = new FrameGenerator[FrameType.values().length];
        this.generators[FrameType.HEADERS.getType()] = new HeadersGenerator(headerGenerator, hpackEncoder, maxHeaderBlockFragment);
        this.generators[FrameType.PRIORITY.getType()] = new PriorityGenerator(headerGenerator);
        this.generators[FrameType.RST_STREAM.getType()] = new ResetGenerator(headerGenerator);
        this.generators[FrameType.SETTINGS.getType()] = new SettingsGenerator(headerGenerator);
        this.generators[FrameType.PUSH_PROMISE.getType()] = new PushPromiseGenerator(headerGenerator, hpackEncoder);
        this.generators[FrameType.PING.getType()] = new PingGenerator(headerGenerator);
        this.generators[FrameType.GO_AWAY.getType()] = new GoAwayGenerator(headerGenerator);
        this.generators[FrameType.WINDOW_UPDATE.getType()] = new WindowUpdateGenerator(headerGenerator);
        this.generators[FrameType.CONTINUATION.getType()] = null; // Never generated explicitly.
        this.generators[FrameType.PREFACE.getType()] = new PrefaceGenerator();
        this.generators[FrameType.DISCONNECT.getType()] = new DisconnectGenerator();

        this.dataGenerator = new DataGenerator(headerGenerator);
    }

    public void setHeaderTableSize(int headerTableSize) {
        hpackEncoder.setRemoteMaxDynamicTableSize(headerTableSize);
    }

    public void setMaxFrameSize(int maxFrameSize) {
        headerGenerator.setMaxFrameSize(maxFrameSize);
    }

    @SuppressWarnings("unchecked")
    public <T extends FrameGenerator> T getControlGenerator(FrameType type) {
        return (T) this.generators[type.getType()];
    }

    public List<ByteBuffer> control(Frame frame) {
        return generators[frame.getType().getType()].generate(frame);
    }

    /**
     * Encode data frame to binary codes
     *
     * @param frame     DataFrame
     * @param maxLength The max length of DataFrame
     * @return A pair of encoding result. The first field is frame length which contains header frame and data frame.
     * The second field is binary codes.
     */
    public Pair<Integer, List<ByteBuffer>> data(DataFrame frame, int maxLength) {
        return dataGenerator.generate(frame, maxLength);
    }

    public void setMaxHeaderListSize(int value) {
        hpackEncoder.setMaxHeaderListSize(value);
    }
}
