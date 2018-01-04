package com.firefly.codec.websocket.stream.impl;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.PongFrame;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.stream.IOState;
import com.firefly.codec.websocket.stream.WebSocketConnection;
import com.firefly.codec.websocket.stream.WebSocketPolicy;
import com.firefly.net.ByteBufferOutputEntry;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class WebSocketConnectionImpl extends AbstractConnection implements WebSocketConnection, IncomingFrames {

    protected Action1<WebSocketConnection> closedListener;
    protected Action2<WebSocketConnection, Throwable> exceptionListener;
    protected IncomingFrames incomingFrames;
    protected final Parser parser;
    protected final Generator generator;
    protected final WebSocketPolicy policy;
    protected final MetaData.Request upgradeRequest;
    protected final MetaData.Response upgradeResponse;

    public WebSocketConnectionImpl(SecureSession secureSession, Session tcpSession, IncomingFrames incomingFrames, WebSocketPolicy policy,
                                   MetaData.Request upgradeRequest, MetaData.Response upgradeResponse) {
        super(secureSession, tcpSession);

        parser = new Parser(policy);
        parser.setIncomingFramesHandler(this);
        generator = new Generator(policy);
        this.policy = policy;
        this.incomingFrames = incomingFrames;
        this.upgradeRequest = upgradeRequest;
        this.upgradeResponse = upgradeResponse;
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
    public IOState getIOState() {
        return null;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return policy;
    }

    @Override
    public void outgoingFrame(Frame frame, Callback callback) {
        ByteBuffer buf = ByteBuffer.allocate(Generator.MAX_HEADER_LENGTH + frame.getPayloadLength());
        generator.generateWholeFrame(frame, buf);
        BufferUtils.flipToFlush(buf, 0);
        tcpSession.encode(new ByteBufferOutputEntry(callback, buf));
    }

    public void setIncomingFrames(IncomingFrames incomingFrames) {
        this.incomingFrames = incomingFrames;
    }

    @Override
    public void incomingError(Throwable t) {
        Optional.ofNullable(incomingFrames).ifPresent(e -> e.incomingError(t));
    }

    @Override
    public void incomingFrame(Frame frame) {
        switch (frame.getType()) {
            case PING: {
                PongFrame pongFrame = new PongFrame();
                outgoingFrame(pongFrame, Callback.NOOP);
            }
            break;
            case CLOSE: {

            }
            break;
        }
        Optional.ofNullable(incomingFrames).ifPresent(e -> e.incomingFrame(frame));
    }
}
