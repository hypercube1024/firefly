package com.fireflysource.net.websocket.client;

import com.fireflysource.net.websocket.common.WebSocketMessageHandler;
import com.fireflysource.net.websocket.common.model.WebSocketPolicy;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class WebSocketClientRequest {

    private String url;
    private WebSocketPolicy policy;
    private List<String> extensions;
    private List<String> subProtocols;
    private WebSocketMessageHandler handler;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public WebSocketPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(WebSocketPolicy policy) {
        this.policy = policy;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public List<String> getSubProtocols() {
        return subProtocols;
    }

    public void setSubProtocols(List<String> subProtocols) {
        this.subProtocols = subProtocols;
    }

    public WebSocketMessageHandler getHandler() {
        return handler;
    }

    public void setHandler(WebSocketMessageHandler handler) {
        this.handler = handler;
    }
}
