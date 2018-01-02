package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.flex.decode.MetaInfoParser;
import com.firefly.net.tcp.codec.flex.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.model.Response;
import com.firefly.net.tcp.codec.flex.protocol.ControlFrame;
import com.firefly.net.tcp.codec.flex.protocol.DataFrame;
import com.firefly.net.tcp.codec.flex.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.flex.protocol.PingFrame;
import com.firefly.net.tcp.codec.flex.stream.FlexConfiguration;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.Session;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class FlexConnectionImpl implements FlexConnection {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    public static final String CONTEXT_KEY = "_context";
    public static final String CTX_LISTENER_KEY = "_contextListener";

    protected final TcpConnection tcpConnection;
    protected final Session session;
    protected final FlexConfiguration configuration;

    public FlexConnectionImpl(FlexConfiguration configuration, TcpConnection tcpConnection, Session session) {
        this.tcpConnection = tcpConnection;
        this.session = session;
        this.configuration = configuration;
    }

    @Override
    public Object getAttachment() {
        return tcpConnection.getAttachment();
    }

    @Override
    public void setAttachment(Object object) {
        tcpConnection.setAttachment(object);
    }

    @Override
    public int getSessionId() {
        return tcpConnection.getSessionId();
    }

    @Override
    public long getOpenTime() {
        return tcpConnection.getOpenTime();
    }

    @Override
    public long getCloseTime() {
        return tcpConnection.getCloseTime();
    }

    @Override
    public long getDuration() {
        return tcpConnection.getDuration();
    }

    @Override
    public long getLastReadTime() {
        return tcpConnection.getLastReadTime();
    }

    @Override
    public long getLastWrittenTime() {
        return tcpConnection.getLastWrittenTime();
    }

    @Override
    public long getLastActiveTime() {
        return tcpConnection.getLastActiveTime();
    }

    @Override
    public long getReadBytes() {
        return tcpConnection.getReadBytes();
    }

    @Override
    public long getWrittenBytes() {
        return tcpConnection.getWrittenBytes();
    }

    @Override
    public long getIdleTimeout() {
        return tcpConnection.getIdleTimeout();
    }

    @Override
    public long getMaxIdleTimeout() {
        return tcpConnection.getMaxIdleTimeout();
    }

    @Override
    public boolean isOpen() {
        return tcpConnection.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tcpConnection.isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return tcpConnection.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return tcpConnection.getRemoteAddress();
    }

    @Override
    public void close() throws IOException {
        tcpConnection.close();
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void newRequest(Request request, Listener listener) {
        Assert.notNull(request, "The request must be not null");
        Assert.notNull(listener, "The context listener must be not null");

        byte[] data = generate(request);
        Stream newLocalStream = getSession().newStream(
                new ControlFrame(false, 0, true, data),
                Callback.NOOP, new NewRequestStreamListener(listener));

        FlexContext context = new FlexContext(request, newLocalStream, FlexConnectionImpl.this);
        newLocalStream.setAttribute(CONTEXT_KEY, context);
        newLocalStream.setAttribute(CTX_LISTENER_KEY, listener);
        listener.newRequest(context);
    }

    private byte[] generate(Request request) {
        return Optional.ofNullable(configuration.getMetaInfoGenerator())
                       .orElse(MetaInfoGenerator.DEFAULT)
                       .generate(request);
    }

    @Override
    public void onRequest(Listener listener) {
        Assert.notNull(listener, "The context listener must be not null");
        session.setListener(new ReceivedRequestSessionListener(listener));
    }

    @Override
    public FlexConfiguration getConfiguration() {
        return configuration;
    }

    protected FlexContext getContext(Stream stream) {
        return (FlexContext) stream.getAttribute(CONTEXT_KEY);
    }

    protected void onDataFrame(Stream stream, DataFrame dataFrame, Listener listener) {
        FlexContext context = getContext(stream);
        Assert.state(context != null, "The flex context has not been created");

        try {
            if (dataFrame.isEndFrame()) {
                Optional.ofNullable(dataFrame.getData()).ifPresent(data -> listener.content(context, data));
                listener.contentComplete(context);

                if (dataFrame.isEndStream()) {
                    listener.messageComplete(context);
                }
            } else {
                Optional.ofNullable(dataFrame.getData()).ifPresent(data -> listener.content(context, data));
            }
        } catch (Exception e) {
            listener.exception(context, e);
        }
    }

    protected class ReceivedRequestSessionListener implements Session.Listener {

        protected final Listener listener;
        protected ByteArrayOutputStream metaInfoByteArrayOutputStream = new ByteArrayOutputStream();

        public ReceivedRequestSessionListener(Listener listener) {
            this.listener = listener;
        }

        protected FlexContext createContext(Stream stream) {
            IO.close(metaInfoByteArrayOutputStream);
            Request request = Optional.ofNullable(configuration.getMetaInfoParser()).orElse(MetaInfoParser.DEFAULT)
                                      .parse(metaInfoByteArrayOutputStream.toByteArray(), Request.class);
            Assert.state(request != null, "Parse request meta info failure");

            return new FlexContext(request, stream, FlexConnectionImpl.this);
        }

        protected void saveData(byte[] data) {
            try {
                metaInfoByteArrayOutputStream.write(data);
            } catch (IOException ignored) {
            }
        }

        protected void onControlFrame(Stream stream, ControlFrame controlFrame) {
            if (controlFrame.isEndFrame()) {
                saveData(controlFrame.getData());
                FlexContext context = createContext(stream);
                stream.setAttribute(CONTEXT_KEY, context);
                stream.setAttribute(CTX_LISTENER_KEY, listener);

                try {
                    listener.newRequest(context);
                    if (controlFrame.isEndStream()) {
                        listener.messageComplete(context);
                    }
                } catch (Exception e) {
                    listener.exception(context, e);
                }
            } else {
                saveData(controlFrame.getData());
            }
        }

        @Override
        public Stream.Listener onNewStream(Stream stream, ControlFrame newControlFrame) {
            onControlFrame(stream, newControlFrame);

            return new Stream.Listener() {

                @Override
                public void onControl(ControlFrame controlFrame) {
                    onControlFrame(stream, controlFrame);
                }

                @Override
                public void onData(DataFrame dataFrame) {
                    onDataFrame(stream, dataFrame, listener);
                }
            };
        }

        @Override
        public void onPing(Session session, PingFrame pingFrame) {
            if (log.isDebugEnabled()) {
                log.debug("Connection {} received ping {}", FlexConnectionImpl.this.getSessionId(), pingFrame.toString());
            }
        }

        @Override
        public void onDisconnect(Session session, DisconnectionFrame disconnectionFrame) {
            IO.close(FlexConnectionImpl.this);
        }
    }

    protected class NewRequestStreamListener implements Stream.Listener {

        protected final Listener listener;
        protected ByteArrayOutputStream metaInfoByteArrayOutputStream = new ByteArrayOutputStream();

        public NewRequestStreamListener(Listener listener) {
            this.listener = listener;
        }

        protected void saveData(byte[] data) {
            try {
                metaInfoByteArrayOutputStream.write(data);
            } catch (IOException ignored) {
            }
        }

        @Override
        public void onControl(ControlFrame controlFrame) {
            if (controlFrame.isEndFrame()) {
                saveData(controlFrame.getData());
                IO.close(metaInfoByteArrayOutputStream);
                Stream stream = getSession().getStream(controlFrame.getStreamId());
                Assert.state(stream != null, "The stream has not been created");

                FlexContext context = getContext(stream);
                Assert.state(context != null, "The flex context has not been created");

                context.setResponse(Optional.ofNullable(configuration.getMetaInfoParser()).orElse(MetaInfoParser.DEFAULT)
                                            .parse(metaInfoByteArrayOutputStream.toByteArray(), Response.class));
                Assert.state(context.getResponse() != null, "Parse response meta info failure");

                try {
                    listener.newResponse(context);

                    if (controlFrame.isEndStream()) {
                        listener.messageComplete(context);
                    }
                } catch (Exception e) {
                    listener.exception(context, e);
                }
            } else {
                saveData(controlFrame.getData());
            }
        }

        @Override
        public void onData(DataFrame dataFrame) {
            Stream stream = getSession().getStream(dataFrame.getStreamId());
            Assert.state(stream != null, "The stream has not been created. id: " + dataFrame.getStreamId());

            onDataFrame(stream, dataFrame, listener);
        }
    }
}
