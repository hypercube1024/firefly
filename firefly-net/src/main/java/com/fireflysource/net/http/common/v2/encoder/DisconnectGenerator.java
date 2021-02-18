package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.net.http.common.v2.frame.Frame;

public class DisconnectGenerator extends FrameGenerator {
    public DisconnectGenerator() {
        super(null);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        return FrameBytes.EMPTY;
    }
}
