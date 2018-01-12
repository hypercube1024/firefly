package com.firefly.server.http2;

import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.model.*;
import com.firefly.server.http2.HTTP1ServerConnection.HTTP1ServerResponseOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class HTTP1ServerRequestHandler implements RequestHandler {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected MetaData.Request request;
    protected MetaData.Response response;
    protected HTTP1ServerConnection connection;
    protected HTTP1ServerResponseOutputStream outputStream;
    protected final ServerHTTPHandler serverHTTPHandler;
    protected HttpFields trailer;

    HTTP1ServerRequestHandler(ServerHTTPHandler serverHTTPHandler) {
        this.serverHTTPHandler = serverHTTPHandler;
    }

    @Override
    public boolean startRequest(String method, String uri, HttpVersion version) {
        if (log.isDebugEnabled()) {
            log.debug("server received the request line, {}, {}, {}", method, uri, version);
        }

        request = new HTTPServerRequest(method, uri, version);
        response = new HTTPServerResponse();
        outputStream = new HTTP1ServerResponseOutputStream(response, connection);

        return HttpMethod.PRI.is(method) && connection.directUpgradeHTTP2(request);
    }

    @Override
    public void parsedHeader(HttpField field) {
        request.getFields().add(field);
    }

    @Override
    public boolean headerComplete() {
        if (HttpMethod.CONNECT.asString().equalsIgnoreCase(request.getMethod())) {
            return serverHTTPHandler.acceptHTTPTunnelConnection(request, response, outputStream, connection);
        } else {
            String expectedValue = request.getFields().get(HttpHeader.EXPECT);
            if ("100-continue".equalsIgnoreCase(expectedValue)) {
                boolean skipNext = serverHTTPHandler.accept100Continue(request, response, outputStream, connection);
                if (skipNext) {
                    return true;
                } else {
                    connection.response100Continue();
                    return serverHTTPHandler.headerComplete(request, response, outputStream, connection);
                }
            } else {
                return serverHTTPHandler.headerComplete(request, response, outputStream, connection);
            }
        }
    }

    @Override
    public boolean content(ByteBuffer item) {
        return serverHTTPHandler.content(item, request, response, outputStream, connection);
    }

    @Override
    public boolean contentComplete() {
        return serverHTTPHandler.contentComplete(request, response, outputStream, connection);
    }

    @Override
    public void parsedTrailer(HttpField field) {
        if (trailer == null) {
            trailer = new HttpFields();
            request.setTrailerSupplier(() -> trailer);
        }
        trailer.add(field);
    }

    @Override
    public boolean messageComplete() {
        try {
            if (connection.getUpgradeHTTP2Complete() || connection.getUpgradeWebSocketComplete()) {
                return true;
            } else {
                boolean success = connection.upgradeProtocol(request, response, outputStream, connection);
                return success || serverHTTPHandler.messageComplete(request, response, outputStream, connection);
            }
        } finally {
            connection.getParser().reset();
        }
    }

    @Override
    public void badMessage(int status, String reason) {
        serverHTTPHandler.badMessage(status, reason, request, response, outputStream, connection);
    }

    @Override
    public void earlyEOF() {
        serverHTTPHandler.earlyEOF(request, response, outputStream, connection);
    }

    @Override
    public int getHeaderCacheSize() {
        return 1024;
    }

}
