package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

abstract public class AbstractHTTP1Connection extends AbstractHTTPConnection {

    protected final HttpParser parser;
    protected final HttpGenerator generator;
    protected final Generator http2Generator;
    protected final HTTP2Configuration config;

    public AbstractHTTP1Connection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
                                   RequestHandler requestHandler, ResponseHandler responseHandler) {
        super(sslSession, tcpSession, HttpVersion.HTTP_1_1);

        this.config = config;
        parser = initHttpParser(config, requestHandler, responseHandler);
        generator = initHttpGenerator();
        http2Generator = new Generator(config.getMaxDynamicTableSize(), config.getMaxHeaderBlockFragment());
    }

    @Override
    public boolean isTunnel() {
        return false;
    }

    abstract protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
                                                 ResponseHandler responseHandler);

    abstract protected HttpGenerator initHttpGenerator();

}
