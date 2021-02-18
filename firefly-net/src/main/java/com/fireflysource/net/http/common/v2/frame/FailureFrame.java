package com.fireflysource.net.http.common.v2.frame;

public class FailureFrame extends Frame {
    private final int error;
    private final String reason;

    public FailureFrame(int error, String reason) {
        super(FrameType.FAILURE);
        this.error = error;
        this.reason = reason;
    }

    public int getError() {
        return error;
    }

    public String getReason() {
        return reason;
    }
}
