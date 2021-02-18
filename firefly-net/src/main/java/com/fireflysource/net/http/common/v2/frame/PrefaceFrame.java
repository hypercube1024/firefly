package com.fireflysource.net.http.common.v2.frame;

import java.nio.charset.StandardCharsets;

public class PrefaceFrame extends Frame {
    /**
     * The bytes of the HTTP/2 preface that form a legal HTTP/1.1
     * request, used in the direct upgrade.
     */
    public static final byte[] PREFACE_PREAMBLE_BYTES = (
            "PRI * HTTP/2.0\r\n" +
                    "\r\n"
    ).getBytes(StandardCharsets.US_ASCII);

    /**
     * The HTTP/2 preface bytes.
     */
    public static final byte[] PREFACE_BYTES = (
            "PRI * HTTP/2.0\r\n" +
                    "\r\n" +
                    "SM\r\n" +
                    "\r\n"
    ).getBytes(StandardCharsets.US_ASCII);

    public PrefaceFrame() {
        super(FrameType.PREFACE);
    }
}
