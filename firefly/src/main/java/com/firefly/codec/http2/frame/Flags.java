package com.firefly.codec.http2.frame;

public interface Flags {
    public static final int NONE = 0x00;
    public static final int END_STREAM = 0x01;
    public static final int ACK = 0x01;
    public static final int END_HEADERS = 0x04;
    public static final int PADDING = 0x08;
    public static final int PRIORITY = 0x20;
}
