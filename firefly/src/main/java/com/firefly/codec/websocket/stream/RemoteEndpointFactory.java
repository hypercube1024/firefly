package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.OutgoingFrames;

public interface RemoteEndpointFactory {
    RemoteEndpoint newRemoteEndpoint(WebSocketConnection connection, OutgoingFrames outgoingFrames, BatchMode batchMode);
}
