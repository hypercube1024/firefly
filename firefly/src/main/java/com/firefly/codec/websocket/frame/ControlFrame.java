package com.firefly.codec.websocket.frame;

import com.firefly.codec.websocket.exception.ProtocolException;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class ControlFrame extends WebSocketFrame {
    /**
     * Maximum size of Control frame, per RFC 6455
     */
    public static final int MAX_CONTROL_PAYLOAD = 125;

    public ControlFrame(byte opcode) {
        super(opcode);
    }

    public void assertValid() {
        if (isControlFrame()) {
            if (getPayloadLength() > ControlFrame.MAX_CONTROL_PAYLOAD) {
                throw new ProtocolException("Desired payload length [" + getPayloadLength() + "] exceeds maximum control payload length ["
                        + MAX_CONTROL_PAYLOAD + "]");
            }

            if ((finRsvOp & 0x80) == 0) {
                throw new ProtocolException("Cannot have FIN==false on Control frames");
            }

            if ((finRsvOp & 0x40) != 0) {
                throw new ProtocolException("Cannot have RSV1==true on Control frames");
            }

            if ((finRsvOp & 0x20) != 0) {
                throw new ProtocolException("Cannot have RSV2==true on Control frames");
            }

            if ((finRsvOp & 0x10) != 0) {
                throw new ProtocolException("Cannot have RSV3==true on Control frames");
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ControlFrame other = (ControlFrame) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (finRsvOp != other.finRsvOp) {
            return false;
        }
        if (!Arrays.equals(mask, other.mask)) {
            return false;
        }
        if (masked != other.masked) {
            return false;
        }
        return true;
    }

    public boolean isControlFrame() {
        return true;
    }

    @Override
    public boolean isDataFrame() {
        return false;
    }

    @Override
    public WebSocketFrame setPayload(ByteBuffer buf) {
        if (buf != null && buf.remaining() > MAX_CONTROL_PAYLOAD) {
            throw new ProtocolException("Control Payloads can not exceed " + MAX_CONTROL_PAYLOAD + " bytes in length.");
        }
        return super.setPayload(buf);
    }

    @Override
    public ByteBuffer getPayload() {
        if (super.getPayload() == null) {
            return BufferUtils.EMPTY_BUFFER;
        }
        return super.getPayload();
    }
}
