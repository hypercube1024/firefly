package com.firefly.codec.http2.stream;

import com.firefly.codec.common.ConnectionType;
import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;

abstract public class AbstractHTTP1Connection extends AbstractHTTPConnection {

    protected final HttpParser parser;
    protected final Generator http2Generator;
    protected final HTTP2Configuration config;

    public AbstractHTTP1Connection(HTTP2Configuration config, SecureSession secureSession, Session tcpSession,
                                   RequestHandler requestHandler, ResponseHandler responseHandler) {
        super(secureSession, tcpSession, HttpVersion.HTTP_1_1);

        this.config = config;
        parser = initHttpParser(config, requestHandler, responseHandler);
        http2Generator = new Generator(config.getMaxDynamicTableSize(), config.getMaxHeaderBlockFragment());
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.HTTP1;
    }

    abstract protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
                                                 ResponseHandler responseHandler);

}
