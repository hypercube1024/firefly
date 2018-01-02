package com.firefly.codec.http2.model;

/**
 * @author Pengtao Qiu
 */
public enum Protocol {
    NONE, H2, WEB_SOCKET;

    public static Protocol from(MetaData.Request request) {
        return getProtocol(request);
    }

    public static Protocol from(MetaData.Response response) {
        if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101) {
            return getProtocol(response);
        } else {
            return NONE;
        }
    }

    private static Protocol getProtocol(MetaData metaData) {
        if (metaData.getFields().contains(HttpHeader.CONNECTION, "Upgrade")) {
            if (metaData.getFields().contains(HttpHeader.UPGRADE, "h2c")) {
                return H2;
            } else if (metaData.getFields().contains(HttpHeader.UPGRADE, "websocket")) {
                return WEB_SOCKET;
            } else {
                return NONE;
            }
        } else {
            return NONE;
        }
    }
}
