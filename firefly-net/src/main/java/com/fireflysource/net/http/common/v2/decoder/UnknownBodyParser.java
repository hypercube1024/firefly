package com.fireflysource.net.http.common.v2.decoder;

import java.nio.ByteBuffer;

public class UnknownBodyParser extends BodyParser {
    private int cursor;

    public UnknownBodyParser(HeaderParser headerParser, Parser.Listener listener) {
        super(headerParser, listener);
    }

    @Override
    public boolean parse(ByteBuffer buffer) {
        int length = cursor == 0 ? getBodyLength() : cursor;
        cursor = consume(buffer, length);
        return cursor == 0;
    }

    private int consume(ByteBuffer buffer, int length) {
        int remaining = buffer.remaining();
        if (remaining >= length) {
            buffer.position(buffer.position() + length);
            return 0;
        } else {
            buffer.position(buffer.limit());
            return length - remaining;
        }
    }
}
