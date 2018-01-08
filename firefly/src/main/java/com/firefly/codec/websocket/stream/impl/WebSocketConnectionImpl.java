package com.firefly.codec.websocket.stream.impl;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.common.ConnectionEvent;
import com.firefly.codec.common.ConnectionType;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.websocket.decode.Parser;
import com.firefly.codec.websocket.encode.Generator;
import com.firefly.codec.websocket.frame.CloseFrame;
import com.firefly.codec.websocket.frame.Frame;
import com.firefly.codec.websocket.frame.PongFrame;
import com.firefly.codec.websocket.frame.WebSocketFrame;
import com.firefly.codec.websocket.model.CloseInfo;
import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.codec.websocket.model.WebSocketBehavior;
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

    public static final byte maskingKey[] = new byte[]{0x01, (byte) 0xfe, 0x22, 0x2d};

    protected final ConnectionEvent<WebSocketConnection> connectionEvent;
    protected IncomingFrames incomingFrames;
    protected final Parser parser;
    protected final Generator generator;
    protected final WebSocketPolicy policy;
    protected final MetaData.Request upgradeRequest;
    protected final MetaData.Response upgradeResponse;
    protected IOState ioState;

    public WebSocketConnectionImpl(SecureSession secureSession, Session tcpSession, IncomingFrames incomingFrames, WebSocketPolicy policy,
                                   MetaData.Request upgradeRequest, MetaData.Response upgradeResponse) {
        super(secureSession, tcpSession);

        connectionEvent = new ConnectionEvent<>(this);
        parser = new Parser(policy);
        parser.setIncomingFramesHandler(this);
        generator = new Generator(policy);
        this.policy = policy;
        this.incomingFrames = incomingFrames;
        this.upgradeRequest = upgradeRequest;
        this.upgradeResponse = upgradeResponse;
        ioState = new IOState();
        ioState.onOpened();
    }

    @Override
    public WebSocketConnection onClose(Action1<WebSocketConnection> closedListener) {
        return connectionEvent.onClose(closedListener);
    }

    @Override
    public WebSocketConnection onException(Action2<WebSocketConnection, Throwable> exceptionListener) {
        return connectionEvent.onException(exceptionListener);
    }

    public void notifyClose() {
        connectionEvent.notifyClose();
    }

    public void notifyException(Throwable t) {
        connectionEvent.notifyException(t);
    }

    @Override
    public IOState getIOState() {
        return ioState;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return policy;
    }

    @Override
    public void outgoingFrame(Frame frame, Callback callback) {
        if (policy.getBehavior() == WebSocketBehavior.CLIENT && frame instanceof WebSocketFrame) {
            WebSocketFrame webSocketFrame = (WebSocketFrame) frame;
            if (!webSocketFrame.isMasked()) {
                webSocketFrame.setMask(maskingKey);
            }
        }
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
                if (policy.getBehavior() == WebSocketBehavior.CLIENT) {
                    pongFrame.setMask(maskingKey);
                }
                outgoingFrame(pongFrame, Callback.NOOP);
            }
            break;
            case CLOSE: {
                CloseFrame closeFrame = (CloseFrame) frame;
                CloseInfo closeInfo = new CloseInfo(closeFrame.getPayload(), false);
                if (policy.getBehavior() == WebSocketBehavior.CLIENT) {
                    closeFrame.setMask(maskingKey);
                }
                ioState.onCloseRemote(closeInfo);
                this.close();
            }
            break;
        }
        Optional.ofNullable(incomingFrames).ifPresent(e -> e.incomingFrame(frame));
    }

    @Override
    public boolean isEncrypted() {
        return secureSession != null;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.WEB_SOCKET;
    }

    public Parser getParser() {
        return parser;
    }

    public Generator getGenerator() {
        return generator;
    }
}
