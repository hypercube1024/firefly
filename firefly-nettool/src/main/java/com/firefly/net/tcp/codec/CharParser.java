package com.firefly.net.tcp.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class CharParser extends AbstractMessageHandler<ByteBuffer, String> {

    private ByteBuffer buffer;
    private CharsetDecoder decoder;

    public CharParser() {
        this("UTF-8");
    }

    public CharParser(String charset) {
        decoder = Charset.forName(charset).newDecoder();
    }

    @Override
    public void parse(ByteBuffer buf) {
        try {
            merge(buf);
            parse();
        } catch (Throwable t) {
            exception.call(t);
        }
    }

    private void merge(ByteBuffer buf) {
        if (buffer != null) {
            if (buffer.hasRemaining()) {
                ByteBuffer tmp = ByteBuffer.allocate(buffer.remaining() + buf.remaining());
                tmp.put(buffer).put(buf).flip();
                buffer = tmp;
            } else {
                buffer = buf;
            }
        } else {
            buffer = buf;
        }
    }

    private void parse() {
        CharBuffer charBuff = allocate();
        while (buffer.hasRemaining()) {
            CoderResult r = decoder.decode(buffer, charBuff, false);
            charBuff.flip();
            if (r.isUnderflow()) {
                if (buffer.hasRemaining()) {
                    buffer = buffer.slice();
                }
                if (charBuff.hasRemaining()) {
                    action.call(charBuff.toString());
                }
                break;
            } else if (r.isOverflow()) {
                action.call(charBuff.toString());
                charBuff = allocate();
            }
        }
    }

    private CharBuffer allocate() {
        int expectedLength = (int) (buffer.remaining() * decoder.averageCharsPerByte()) + 1;
        return CharBuffer.allocate(expectedLength);
    }

}
