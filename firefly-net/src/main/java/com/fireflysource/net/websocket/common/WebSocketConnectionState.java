package com.fireflysource.net.websocket.common;

import com.fireflysource.net.websocket.common.stream.ConnectionState;

/**
 * @author Pengtao Qiu
 */
public interface WebSocketConnectionState {

    boolean isInputAvailable();

    boolean isOutputAvailable();

    ConnectionState getConnectionState();

    boolean isRemoteCloseInitiated();

    boolean isLocalCloseInitiated();

    boolean isAbnormalClose();

    boolean isCleanClose();
}
