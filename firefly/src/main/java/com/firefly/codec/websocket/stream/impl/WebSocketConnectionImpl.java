package com.firefly.codec.websocket.stream.impl;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.model.BatchMode;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.WriteCallback;
import com.firefly.codec.websocket.stream.IOState;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

/**
 * @author Pengtao Qiu
 */
public class WebSocketConnectionImpl extends AbstractConnection implements WebSocketConnection {

    protected Action1<WebSocketConnection> closedListener;
    protected Action2<WebSocketConnection, Throwable> exceptionListener;
    protected final IncomingFrames incomingFrames;
    protected final Parser parser;
    protected final Generator generator;
    protected final WebSocketPolicy policy;

    public WebSocketConnectionImpl(SecureSession secureSession, Session tcpSession, IncomingFrames incomingFrames, WebSocketPolicy policy) {
        super(secureSession, tcpSession);
        this.incomingFrames = incomingFrames;
        parser = new Parser(policy);
        parser.setIncomingFramesHandler(incomingFrames);
        generator = new Generator(policy);
        this.policy = policy;
    }

    @Override
    public WebSocketConnection onClose(Action1<WebSocketConnection> closedListener) {
        this.closedListener = closedListener;
        return this;
    }

    @Override
    public WebSocketConnection onException(Action2<WebSocketConnection, Throwable> exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    @Override
    public void onLocalClose(CloseInfo close) {

    }

    @Override
    public IOState getIOState() {
        return null;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return policy;
    }

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode) {

    }
}
