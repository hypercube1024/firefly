package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.ffsocks.decode.MetaInfoParser;
import com.firefly.net.tcp.codec.ffsocks.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.model.Response;
import com.firefly.net.tcp.codec.ffsocks.protocol.ControlFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.DataFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.DisconnectionFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.PingFrame;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConfiguration;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.codec.ffsocks.stream.Session;
import com.firefly.net.tcp.codec.ffsocks.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.io.IO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractFfsocksConnection implements FfsocksConnection {

    protected final TcpConnection tcpConnection;
    protected final Session session;
    protected final FfsocksConfiguration configuration;

    public AbstractFfsocksConnection(FfsocksConfiguration configuration, TcpConnection tcpConnection, Session session) {
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
        Assert.notNull(listener, "The listener must be not null");

        AtomicReference<FfsocksContext> contextReference = new AtomicReference<>();
        byte[] data = Optional.ofNullable(configuration.getMetaInfoGenerator()).orElse(MetaInfoGenerator.DEFAULT)
                              .generate(request);
        getSession().newStream(new ControlFrame(false, 0, false, data),
                new StreamListener(contextReference, listener)).thenAccept(stream -> {
            FfsocksContext context = new FfsocksContext(request, stream, AbstractFfsocksConnection.this);
            contextReference.set(context);
            listener.newRequest(context);
        });
    }

    @Override
    public void onRequest(Listener listener) {
        Assert.notNull(listener, "The listener must be not null");
        session.setListener(new Session.Listener() {

            protected ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();

            @Override
            public Stream.Listener onNewStream(Stream stream, ControlFrame controlFrame) {

                if (controlFrame.isEndFrame()) {

                } else {
                    try {
                        metaInfo.write(controlFrame.getData());
                    } catch (IOException ignored) {
                    }
                }

                return new Stream.Listener() {

                    @Override
                    public void onControl(ControlFrame controlFrame) {

                    }

                    @Override
                    public void onData(DataFrame dataFrame) {

                    }
                };
            }

            @Override
            public void onPing(Session session, PingFrame pingFrame) {

            }

            @Override
            public void onDisconnect(Session session, DisconnectionFrame disconnectionFrame) {

            }
        });
    }

    @Override
    public FfsocksConfiguration getConfiguration() {
        return configuration;
    }

    protected class StreamListener implements Stream.Listener {

        protected final AtomicReference<FfsocksContext> contextReference;
        protected final Listener listener;
        protected ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();

        public StreamListener(AtomicReference<FfsocksContext> contextReference, Listener listener) {
            this.contextReference = contextReference;
            this.listener = listener;
        }

        @Override
        public void onControl(ControlFrame controlFrame) {
            if (controlFrame.isEndFrame()) {
                IO.close(metaInfo);
                FfsocksContext context = contextReference.get();
                Assert.state(context != null, "The ffsocks context has not been created");

                context.setResponse(Optional.ofNullable(configuration.getMetaInfoParser()).orElse(MetaInfoParser.DEFAULT)
                                            .parse(metaInfo.toByteArray(), Response.class));
                Assert.state(context.getResponse() != null, "Parse response meta info failure");
                listener.newResponse(context);

                if (controlFrame.isEndStream()) {
                    listener.messageComplete(context);
                }
            } else {
                try {
                    metaInfo.write(controlFrame.getData());
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public void onData(DataFrame dataFrame) {
            FfsocksContext context = contextReference.get();
            Assert.state(context != null, "The ffsocks context has not been created");

            if (dataFrame.isEndFrame()) {
                Optional.ofNullable(dataFrame.getData()).ifPresent(data -> listener.content(context, data));
                listener.contentComplete(context);

                if (dataFrame.isEndStream()) {
                    listener.messageComplete(context);
                }
            } else {
                Optional.ofNullable(dataFrame.getData()).ifPresent(data -> listener.content(context, data));
            }
        }
    }
}
