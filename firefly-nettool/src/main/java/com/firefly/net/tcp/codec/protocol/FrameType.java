package com.firefly.net.tcp.codec.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum FrameType {
    CONTROL((byte) 1, "Control frame"),
    DATA((byte) 2, "Data frame"),
    PING((byte) 3, "Ping frame"),
    ERROR((byte) 4, "Error frame");

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

    public static Optional<FrameType> from(byte value) {
        return Optional.ofNullable(Holder.map.get(value));
    }
}
