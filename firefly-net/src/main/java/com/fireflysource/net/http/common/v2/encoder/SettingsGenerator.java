package com.fireflysource.net.http.common.v2.encoder;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.v2.frame.Flags;
import com.fireflysource.net.http.common.v2.frame.Frame;
import com.fireflysource.net.http.common.v2.frame.FrameType;
import com.fireflysource.net.http.common.v2.frame.SettingsFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Map;

public class SettingsGenerator extends FrameGenerator {
    public SettingsGenerator(HeaderGenerator headerGenerator) {
        super(headerGenerator);
    }

    @Override
    public FrameBytes generate(Frame frame) {
        SettingsFrame settingsFrame = (SettingsFrame) frame;
        return generateSettings(settingsFrame.getSettings(), settingsFrame.isReply());
    }

    public FrameBytes generateSettings(Map<Integer, Integer> settings, boolean reply) {
        // Two bytes for the identifier, four bytes for the value.
        int entryLength = 2 + 4;
        int length = entryLength * settings.size();
        if (length > getMaxFrameSize())
            throw new IllegalArgumentException("Invalid settings, too big");

        ByteBuffer header = generateHeader(FrameType.SETTINGS, length, reply ? Flags.ACK : Flags.NONE, 0);

        for (Map.Entry<Integer, Integer> entry : settings.entrySet()) {
            header.putShort(entry.getKey().shortValue());
            header.putInt(entry.getValue());
        }

        BufferUtils.flipToFlush(header, 0);

        FrameBytes frameBytes = new FrameBytes();
        frameBytes.setByteBuffers(new LinkedList<>());
        frameBytes.getByteBuffers().add(header);
        frameBytes.setLength(Frame.HEADER_LENGTH + length);
        return frameBytes;
    }
}
