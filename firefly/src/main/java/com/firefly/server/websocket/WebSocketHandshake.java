package com.firefly.server.websocket;

import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.websocket.model.AcceptHash;
import com.firefly.utils.Assert;
import com.firefly.utils.function.Func4;
import com.firefly.utils.io.IO;

/**
 * @author Pengtao Qiu
 */
public class WebSocketHandshake implements Func4<MetaData.Request, MetaData.Response, HTTPOutputStream, HTTPConnection, Boolean> {

    /**
     * RFC 6455 - Sec-WebSocket-Version
     */
    public static final int VERSION = 13;

    @Override
    public Boolean call(MetaData.Request request, MetaData.Response response,
                        HTTPOutputStream httpOutputStream, HTTPConnection httpConnection) {
        try {
            String key = request.getFields().get("Sec-WebSocket-Key");
            Assert.hasText(key, "Missing request header 'Sec-WebSocket-Key'");

            response.setStatus(HttpStatus.SWITCHING_PROTOCOLS_101);
            response.getFields().put("Upgrade", "WebSocket");
            response.getFields().add("Connection", "Upgrade");
            response.getFields().add("Sec-WebSocket-Accept", AcceptHash.hashKey(key));
            return true;
        } finally {
            IO.close(httpOutputStream);
        }
    }
}
