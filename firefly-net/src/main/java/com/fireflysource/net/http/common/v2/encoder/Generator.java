package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.net.http.common.v2.frame.DataFrame;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.hpack.HpackEncoder;

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

    public FrameBytes control(Frame frame) {
        return generators[frame.getType().getType()].generate(frame);
    }

    public FrameBytes data(DataFrame frame, int maxLength) {
        return dataGenerator.generate(frame, maxLength);
    }

    public void setMaxHeaderListSize(int value) {
        hpackEncoder.setMaxHeaderListSize(value);
    }
}
