package com.fireflysource.net.http.common.v2.frame;

import java.util.HashMap;
import java.util.Map;

public enum FrameType {
    DATA(0),
    HEADERS(1),
    PRIORITY(2),
    RST_STREAM(3),
    SETTINGS(4),
    PUSH_PROMISE(5),
    PING(6),
    GO_AWAY(7),
    WINDOW_UPDATE(8),
    CONTINUATION(9),
    // Synthetic frames only needed by the implementation.
    PREFACE(10),
    DISCONNECT(11),
    FAILURE(12);

    private final int type;

    FrameType(int type) {
        this.type = type;
        Types.types.put(type, this);
    }

    public static FrameType from(int type) {
        return Types.types.get(type);
    }

    public int getType() {
        return type;
    }

    private static class Types {
        private static final Map<Integer, FrameType> types = new HashMap<>();
    }
}
