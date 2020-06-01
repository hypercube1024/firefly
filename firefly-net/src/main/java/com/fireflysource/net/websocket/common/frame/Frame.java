package com.fireflysource.net.websocket.common.frame;

import java.nio.ByteBuffer;

/**
 * An immutable websocket frame.
 */
public interface Frame {
    enum Type {
        CONTINUATION((byte) 0x00),
        TEXT((byte) 0x01),
        BINARY((byte) 0x02),
        CLOSE((byte) 0x08),
        PING((byte) 0x09),
        PONG((byte) 0x0A);

        public static Type from(byte op) {
            for (Type type : values()) {
                if (type.opcode == op) {
                    return type;
                }
            }
            throw new IllegalArgumentException("OpCode " + op + " is not a valid Frame.Type");
        }

        private byte opcode;

        Type(byte code) {
            this.opcode = code;
        }

        public byte getOpCode() {
            return opcode;
        }

        public boolean isControl() {
            return (opcode >= CLOSE.getOpCode());
        }

        public boolean isData() {
            return (opcode == TEXT.getOpCode()) || (opcode == BINARY.getOpCode());
        }

        public boolean isContinuation() {
            return opcode == CONTINUATION.getOpCode();
        }

        @Override
        public String toString() {
            return this.name();
        }
    }

    byte[] getMask();

    byte getOpCode();

    ByteBuffer getPayload();

    /**
     * The original payload length ({@link ByteBuffer#remaining()})
     *
     * @return the original payload length ({@link ByteBuffer#remaining()})
     */
    int getPayloadLength();

    Type getType();

    boolean hasPayload();

    boolean isFin();

    /**
     * Same as {@link #isFin()}
     *
     * @return true if final frame.
     * @deprecated use {@link #isFin()} instead
     */
    @Deprecated
    boolean isLast();

    boolean isMasked();

    boolean isRsv1();

    boolean isRsv2();

    boolean isRsv3();
}
