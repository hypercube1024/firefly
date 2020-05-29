package com.fireflysource.net.websocket.decoder;


import com.fireflysource.net.websocket.model.WebSocketPolicy;

import java.nio.ByteBuffer;

public class UnitParser extends Parser {
    public UnitParser() {
        this(WebSocketPolicy.newServerPolicy());
    }

    public UnitParser(WebSocketPolicy policy) {
        super(policy);
    }

    private void parsePartial(ByteBuffer buf, int numBytes) {
        int len = Math.min(numBytes, buf.remaining());
        byte[] arr = new byte[len];
        buf.get(arr, 0, len);
        this.parse(ByteBuffer.wrap(arr));
    }

    /**
     * Parse a buffer, but do so in a quiet fashion, squelching stacktraces if encountered.
     * <p>
     * Use if you know the parse will cause an exception and just don't want to make the test console all noisy.
     *
     * @param buf the buffer to parse
     */
    public void parseQuietly(ByteBuffer buf) {
        parse(buf);
    }

    public void parseSlowly(ByteBuffer buf, int segmentSize) {
        while (buf.remaining() > 0) {
            parsePartial(buf, segmentSize);
        }
    }
}
