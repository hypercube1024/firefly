package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.PingFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class PingGenerator extends FrameGenerator {

    public PingGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        PingFrame pingFrame = (PingFrame) frame;
        return generatePing(pingFrame.getPayload(), pingFrame.isReply());
    }

    public FrameBytes generatePing(byte[] payload, boolean reply) {
        if (payload.length != PingFrame.PING_LENGTH) {
            throw new IllegalArgumentException("Invalid payload length: " + payload.length);
        }

        ByteBuffer header = generateHeader(FrameType.PING, PingFrame.PING_LENGTH, reply ? Flags.ACK : Flags.NONE, 0);

        header.put(payload);

        BufferUtils.flipToFlush(header, 0);

        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        frameBytes.setLength(Frame.HEADER_LENGTH + PingFrame.PING_LENGTH);
        return frameBytes;
    }
}
