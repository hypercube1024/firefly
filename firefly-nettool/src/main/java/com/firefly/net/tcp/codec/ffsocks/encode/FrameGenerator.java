package com.firefly.net.tcp.codec.ffsocks.encode;

import com.firefly.net.tcp.codec.Generator;
import com.firefly.net.tcp.codec.ffsocks.protocol.Frame;
import com.firefly.net.tcp.codec.ffsocks.protocol.FrameType;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class FrameGenerator {

    public static final FrameHeaderGenerator headerGenerator = new FrameHeaderGenerator();
    private static final Map<FrameType, Generator> generatorMap = new EnumMap<>(FrameType.class);

    static {
        generatorMap.put(FrameType.CONTROL, new MessageFrameGenerator());
        generatorMap.put(FrameType.DATA, new MessageFrameGenerator());
        generatorMap.put(FrameType.PING, new PingGenerator());
        generatorMap.put(FrameType.DISCONNECTION, new DisconnectionFrameGenerator());
    }

    public static ByteBuffer generate(Frame frame) {
        return generatorMap.get(frame.getType()).generate(frame);
    }
}
