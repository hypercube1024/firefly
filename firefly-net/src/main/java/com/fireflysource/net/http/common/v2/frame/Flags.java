package com.fireflysource.net.http.common.v2.frame;

public interface Flags {
    int NONE = 0x00;
    int END_STREAM = 0x01;
    int ACK = 0x01;
    int END_HEADERS = 0x04;
    int PADDING = 0x08;
    int PRIORITY = 0x20;
}
