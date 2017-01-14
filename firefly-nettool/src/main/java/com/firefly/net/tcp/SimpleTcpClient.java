package com.firefly.net.tcp;

import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.AbstractLifeCycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleTcpClient extends AbstractLifeCycle {

    private AsynchronousTcpClient client;
    private TcpConfiguration config;

    private Map<Integer, Promise<TcpConnection>> context = new ConcurrentHashMap<>();

    public SimpleTcpClient() {
        this(new TcpConfiguration());
    }

    public SimpleTcpClient(TcpConfiguration config) {
        client = new AsynchronousTcpClient(config);
        this.config = config;
    }

    public Promise.Completable<TcpConnection> connect(String host, int port) {
        Promise.Completable<TcpConnection> promise = new Promise.Completable<>();
        connect(host, port, promise);
        return promise;
    }

    public void connect(String host, int port, Action1<TcpConnection> conn) {
        Promise<TcpConnection> promise = new Promise<TcpConnection>() {

            @Override
            public void succeeded(TcpConnection result) {
                conn.call(result);
            }
        };
        connect(host, port, promise);
    }

    public void connect(String host, int port, Action1<TcpConnection> conn, Action1<Throwable> failed) {
        Promise<TcpConnection> promise = new Promise<TcpConnection>() {

            @Override
            public void succeeded(TcpConnection result) {
                conn.call(result);
            }

            @Override
            public void failed(Throwable x) {
                failed.call(x);
            }
        };
        connect(host, port, promise);
    }

    public void connect(String host, int port, Promise<TcpConnection> promise) {
        start();
        int sessionId = client.connect(host, port);

        context.put(sessionId, promise);
    }

    public abstract class AbstractHandler extends AbstractSimpleHandler {

        @Override
        public void failedOpeningSession(Integer sessionId, Throwable t) throws Throwable {
            try {
                Promise<TcpConnection> promise = context.get(sessionId);
                if (promise != null) {
                    promise.failed(t);
                }
            } finally {
                context.remove(sessionId);
            }
        }

        @Override
        public void sessionClosed(Session session) throws Throwable {
            try {
                super.sessionClosed(session);
            } finally {
                context.remove(session.getSessionId());
            }
        }

        @Override
        public void exceptionCaught(Session session, Throwable t) throws Throwable {
            try {
                super.exceptionCaught(session, t);
            } finally {
                context.remove(session.getSessionId());
            }
        }
    }

    @Override
    protected void init() {
        if (!config.isSecureConnectionEnabled()) {
            config.setDecoder(AbstractSimpleHandler.decoder);
            config.setHandler(new AbstractHandler() {

                @Override
                public void sessionOpened(Session session) throws Throwable {
                    TcpConnectionImpl c = new TcpConnectionImpl(session);
                    session.attachObject(c);
                    sessionOpen(session, c);
                }

            });
        } else {
            config.setDecoder(AbstractSimpleHandler.sslDecoder);
            config.setHandler(new AbstractHandler() {

                @Override
                public void sessionOpened(Session session) throws Throwable {
                    session.attachObject(new SecureTcpConnectionImpl(session, new SSLSession(config.getSslContextFactory(), true, session, (ssl) -> {
                        Object o = session.getAttachment();
                        if (o != null && o instanceof SecureTcpConnectionImpl) {
                            SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
                            sessionOpen(session, c);
                        }
                    })));
                }

                @Override
                public void sessionClosed(Session session) throws Throwable {
                    try {
                        super.sslSessionClosed(session);
                    } finally {
                        context.remove(session.getSessionId());
                    }
                }
            });
        }
    }

    private void sessionOpen(Session session, TcpConnection c) {
        try {
            Promise<TcpConnection> promise = context.get(session.getSessionId());
            if (promise != null) {
                promise.succeeded(c);
            }
        } finally {
            context.remove(session.getSessionId());
        }
    }

    @Override
    protected void destroy() {
        client.stop();
    }

}
