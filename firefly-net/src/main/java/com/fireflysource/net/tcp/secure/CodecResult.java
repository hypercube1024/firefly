package com.fireflysource.net.tcp.secure;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class CodecResult {

    private final ByteBuffer byteBuffer;
    private final Status status;

    public CodecResult(ByteBuffer byteBuffer, Status status) {
        this.byteBuffer = byteBuffer;
        this.status = status;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        NEED_MORE_DATA, SUCCESS
    }
}
