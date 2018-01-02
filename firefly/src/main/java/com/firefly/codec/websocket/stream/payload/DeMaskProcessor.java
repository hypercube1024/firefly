package com.firefly.codec.websocket.stream.payload;

import com.firefly.codec.websocket.frame.Frame;

import java.nio.ByteBuffer;

public class DeMaskProcessor implements PayloadProcessor {
    private byte[] maskBytes;
    private int maskInt;
    private int maskOffset;

    @Override
    public void process(ByteBuffer payload) {
        if (maskBytes == null) {
            return;
        }

        int maskInt = this.maskInt;
        int start = payload.position();
        int end = payload.limit();
        int offset = this.maskOffset;
        int remaining;
        while ((remaining = end - start) > 0) {
            if (remaining >= 4 && (offset & 3) == 0) {
                payload.putInt(start, payload.getInt(start) ^ maskInt);
                start += 4;
                offset += 4;
            } else {
                payload.put(start, (byte) (payload.get(start) ^ maskBytes[offset & 3]));
                ++start;
                ++offset;
            }
        }
        maskOffset = offset;
    }

    public void reset(byte[] mask) {
        this.maskBytes = mask;
        int maskInt = 0;
        if (mask != null) {
            for (byte maskByte : mask)
                maskInt = (maskInt << 8) + (maskByte & 0xFF);
        }
        this.maskInt = maskInt;
        this.maskOffset = 0;
    }

    @Override
    public void reset(Frame frame) {
        reset(frame.getMask());
    }
}
