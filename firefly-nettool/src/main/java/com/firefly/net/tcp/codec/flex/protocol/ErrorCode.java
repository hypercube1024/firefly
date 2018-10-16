package com.firefly.net.tcp.codec.flex.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public enum ErrorCode {

    NO_ERROR((byte) 0, "No error"),
    INTERNAL((byte) 1, "Internal error"),
    BAD_MESSAGE((byte) 2, "Protocol format error"),
    IO_ERROR((byte) 3, "I/O error");

    private final byte value;
    private final String description;

    static class Holder {
        static final Map<Byte, ErrorCode> map = new HashMap<>();
    }

    ErrorCode(byte value, String description) {
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
