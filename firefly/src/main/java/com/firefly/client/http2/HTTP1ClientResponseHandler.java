package com.firefly.client.http2;

import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class HTTP1ClientResponseHandler implements ResponseHandler {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected HTTP1ClientConnection connection;
    protected MetaData.Response response;
    protected MetaData.Request request;
    protected HTTPOutputStream outputStream;
    protected final ClientHTTPHandler clientHTTPHandler;
    protected HttpFields trailer;

    HTTP1ClientResponseHandler(ClientHTTPHandler clientHTTPHandler) {
        this.clientHTTPHandler = clientHTTPHandler;
    }

    @Override
    public final boolean startResponse(HttpVersion version, int status, String reason) {
        if (log.isDebugEnabled()) {
            log.debug("client received the response line, {}, {}, {}", version, status, reason);
        }

        if (status == HttpStatus.CONTINUE_100 && HttpStatus.Code.CONTINUE.getMessage().equalsIgnoreCase(reason)) {
            clientHTTPHandler.continueToSendData(request, response, outputStream, connection);
            if (log.isDebugEnabled()) {
                log.debug("client received 100 continue, current parser state is {}", connection.getParser().getState());
            }
            return true;
        } else {
            response = new HTTPClientResponse(version, status, reason);
            return false;
        }
    }

    @Override
    public final void parsedHeader(HttpField field) {
        response.getFields().add(field);
    }

    @Override
    public final int getHeaderCacheSize() {
        return 1024;
    }

    @Override
    public final boolean headerComplete() {
        return clientHTTPHandler.headerComplete(request, response, outputStream, connection);
    }

    @Override
    public final boolean content(ByteBuffer item) {
        return clientHTTPHandler.content(item, request, response, outputStream, connection);
    }

    @Override
    public boolean contentComplete() {
        return clientHTTPHandler.contentComplete(request, response, outputStream, connection);
    }

    @Override
    public void parsedTrailer(HttpField field) {
        if (trailer == null) {
            trailer = new HttpFields();
            response.setTrailerSupplier(() -> trailer);
        }
        trailer.add(field);
    }

    protected boolean http1MessageComplete() {
        try {
            return clientHTTPHandler.messageComplete(request, response, outputStream, connection);
        } finally {
            String requestConnectionValue = request.getFields().get(HttpHeader.CONNECTION);
            String responseConnectionValue = response.getFields().get(HttpHeader.CONNECTION);

            connection.getParser().reset();

            switch (response.getHttpVersion()) {
                case HTTP_1_0:
                    if ("keep-alive".equalsIgnoreCase(requestConnectionValue)
                            && "keep-alive".equalsIgnoreCase(responseConnectionValue)) {
                        log.debug("the client {} connection is persistent", response.getHttpVersion());
                    } else {
                        IO.close(connection);
                    }
                    break;
                case HTTP_1_1: // the persistent connection is default in HTTP 1.1
                    if ("close".equalsIgnoreCase(requestConnectionValue)
                            || "close".equalsIgnoreCase(responseConnectionValue)) {
                        IO.close(connection);
                    } else {
                        log.debug("the client {} connection is persistent", response.getHttpVersion());
                    }
                    break;
            }

        }
    }

    @Override
    public final boolean messageComplete() {
        boolean success = connection.upgradeHTTP2Complete(response);
        if (success) {
            log.debug("client upgraded http2 successfully");
        }
        return http1MessageComplete();
    }

    @Override
    public final void badMessage(int status, String reason) {
        clientHTTPHandler.badMessage(status, reason, request, response, outputStream, connection);
    }

    @Override
    public void earlyEOF() {
        clientHTTPHandler.earlyEOF(request, response, outputStream, connection);
    }

}
