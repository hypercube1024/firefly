package com.firefly.codec.http2.stream;

import com.firefly.codec.common.ConnectionType;
import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;

abstract public class AbstractHTTP2Connection extends AbstractHTTPConnection {

    protected final HTTP2Session http2Session;
    protected final Parser parser;
    protected final Generator generator;

    public AbstractHTTP2Connection(HTTP2Configuration config,
                                   Session tcpSession, SecureSession secureSession,
                                   Listener listener) {
        super(secureSession, tcpSession, HttpVersion.HTTP_2);

        FlowControlStrategy flowControl;
        switch (config.getFlowControlStrategy()) {
            case "buffer":
                flowControl = new BufferingFlowControlStrategy(config.getInitialStreamSendWindow(), 0.5f);
                break;
            case "simple":
                flowControl = new SimpleFlowControlStrategy(config.getInitialStreamSendWindow());
                break;
            default:
                flowControl = new SimpleFlowControlStrategy(config.getInitialStreamSendWindow());
                break;
        }
        this.generator = new Generator(config.getMaxDynamicTableSize(), config.getMaxHeaderBlockFragment());
        this.http2Session = initHTTP2Session(config, flowControl, listener);
        this.parser = initParser(config);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.HTTP2;
    }

    abstract protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
                                                     Listener listener);

    abstract protected Parser initParser(HTTP2Configuration config);

    public com.firefly.codec.http2.stream.Session getHttp2Session() {
        return http2Session;
    }

}
