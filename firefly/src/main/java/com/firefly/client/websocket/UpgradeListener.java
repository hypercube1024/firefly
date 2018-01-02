package com.firefly.client.websocket;


import com.firefly.codec.websocket.model.UpgradeRequest;
import com.firefly.codec.websocket.model.UpgradeResponse;

/**
 * Listener for Handshake/Upgrade events.
 */
public interface UpgradeListener {
    void onHandshakeRequest(UpgradeRequest request);

    void onHandshakeResponse(UpgradeResponse response);
}
