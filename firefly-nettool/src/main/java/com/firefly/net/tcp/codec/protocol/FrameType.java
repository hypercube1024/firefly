package com.firefly.net.tcp.codec.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public enum FrameType {
    CONTROL((byte) 1, "Control Frame"),
    DATA((byte) 2, "Data Frame"),
    PING((byte) 3, "Ping Frame");

    private final byte value;
    private final String description;

    static class Holder {
        static final Map<Byte, FrameType> map = new HashMap<>();
    }

    FrameType(byte value, String description) {
        this.value = value;
        this.description = description;
        Holder.map.put(value, this);
    }

    public byte getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
