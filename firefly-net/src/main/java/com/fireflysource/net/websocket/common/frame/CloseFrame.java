package com.fireflysource.net.websocket.common.frame;

import com.fireflysource.common.string.StringUtils;
import com.fireflysource.net.websocket.common.model.OpCode;

public class CloseFrame extends ControlFrame {
    public CloseFrame() {
        super(OpCode.CLOSE);
    }

    @Override
    public Type getType() {
        return Type.CLOSE;
    }

    /**
     * Truncate arbitrary reason into something that will fit into the CloseFrame limits.
     *
     * @param reason the arbitrary reason to possibly truncate.
     * @return the possibly truncated reason string.
     */
    public static String truncate(String reason) {
        return StringUtils.truncate(reason, (ControlFrame.MAX_CONTROL_PAYLOAD - 2));
    }
}
