package com.firefly.codec.websocket.frame;

import java.nio.ByteBuffer;

/**
 * Immutable, Read-only, Frame implementation.
 */
public class ReadOnlyDelegatedFrame implements Frame {
    private final Frame delegate;

    public ReadOnlyDelegatedFrame(Frame frame) {
        this.delegate = frame;
    }

    @Override
    public byte[] getMask() {
        return delegate.getMask();
    }

    @Override
    public byte getOpCode() {
        return delegate.getOpCode();
    }

    @Override
    public ByteBuffer getPayload() {
        if (!delegate.hasPayload()) {
            return null;
        }
        return delegate.getPayload().asReadOnlyBuffer();
    }

    @Override
    public int getPayloadLength() {
        return delegate.getPayloadLength();
    }

    @Override
    public Type getType() {
        return delegate.getType();
    }

    @Override
    public boolean hasPayload() {
        return delegate.hasPayload();
    }

    @Override
    public boolean isFin() {
        return delegate.isFin();
    }

    @Override
    @Deprecated
    public boolean isLast() {
        return delegate.isLast();
    }

    @Override
    public boolean isMasked() {
        return delegate.isMasked();
    }

    @Override
    public boolean isRsv1() {
        return delegate.isRsv1();
    }

    @Override
    public boolean isRsv2() {
        return delegate.isRsv2();
    }

    @Override
    public boolean isRsv3() {
        return delegate.isRsv3();
    }
}
