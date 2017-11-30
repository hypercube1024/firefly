package com.firefly.codec.http2.model;

import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public enum Protocol {
    NONE, H2, WEB_SOCKET;

    public static Protocol from(MetaData.Request request) {
        if (request.getFields().contains(HttpHeader.CONNECTION, "Upgrade")) {
            if (request.getFields().contains(HttpHeader.UPGRADE, "h2c")) {
                return H2;
            } else if (request.getFields().contains(HttpHeader.UPGRADE, "websocket")) {
                return WEB_SOCKET;
            } else {
                return NONE;
            }
        } else {
            return NONE;
        }
    }
}
