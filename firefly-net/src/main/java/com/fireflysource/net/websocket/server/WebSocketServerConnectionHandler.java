package com.fireflysource.net.websocket.server;

import com.fireflysource.net.websocket.common.WebSocketMessageHandler;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;

/**
 * @author Pengtao Qiu
 */
public class WebSocketServerConnectionHandler {

    private String url;
    private ExtensionSelector extensionSelector;
    private SubProtocolSelector subProtocolSelector;
    private WebSocketPolicy policy;
    private WebSocketServerConnectionListener connectionListener;
    private WebSocketMessageHandler messageHandler;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ExtensionSelector getExtensionSelector() {
        return extensionSelector;
    }

    public void setExtensionSelector(ExtensionSelector extensionSelector) {
        this.extensionSelector = extensionSelector;
    }

    public SubProtocolSelector getSubProtocolSelector() {
        return subProtocolSelector;
    }

    public void setSubProtocolSelector(SubProtocolSelector subProtocolSelector) {
        this.subProtocolSelector = subProtocolSelector;
    }

    public WebSocketPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(WebSocketPolicy policy) {
        this.policy = policy;
    }

    public WebSocketServerConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(WebSocketServerConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public WebSocketMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(WebSocketMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }
}
