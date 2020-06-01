package com.fireflysource.net.websocket.common.extension;


import com.fireflysource.net.websocket.common.model.WebSocketPolicy;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractExtensionTest {

    protected ExtensionTool clientExtensions;
    protected ExtensionTool serverExtensions;

    @BeforeEach
    public void init() {
        clientExtensions = new ExtensionTool(WebSocketPolicy.newClientPolicy());
        serverExtensions = new ExtensionTool(WebSocketPolicy.newServerPolicy());
    }
}
