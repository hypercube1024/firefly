package com.fireflysource.net.http.common.v2.frame;

public abstract class Frame {
    public static final int HEADER_LENGTH = 9;
    public static final int DEFAULT_MAX_LENGTH = 0x40_00;
    public static final int MAX_MAX_LENGTH = 0xFF_FF_FF;
    public static final Frame[] EMPTY_ARRAY = new Frame[0];

    private final FrameType type;

    protected Frame(FrameType type) {
        this.type = type;
    }

    public FrameType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s@%x", getClass().getSimpleName(), hashCode());
    }
}
