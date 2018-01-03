package com.firefly.codec.websocket.stream;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractWebSocketConnection extends AbstractConnection implements WebSocketConnection {

    protected Action1<WebSocketConnection> closedListener;
    protected Action2<WebSocketConnection, Throwable> exceptionListener;
    protected IncomingFrames incomingFrames;

    public AbstractWebSocketConnection(SecureSession secureSession, Session tcpSession, IncomingFrames incomingFrames) {
        super(secureSession, tcpSession);
        this.incomingFrames = incomingFrames;
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
    public void setIncomingFrames(IncomingFrames incomingFrames) {
        this.incomingFrames = incomingFrames;
    }

    @Override
    public IncomingFrames getIncomingFrames() {
        return incomingFrames;
    }

}
