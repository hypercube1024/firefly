package com.firefly.net.tcp;

import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Func0;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.eclipse.jetty.alpn.ALPN;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleTcpClient extends AbstractLifeCycle {

    // ALPN callback
    private Action0 alpnUnsupported;
    private Action1<String> alpnSelected;
    private Func0<List<String>> alpnProtocols;

    private AsynchronousTcpClient client;
    private TcpConfiguration config;

    private Map<Integer, ClientContext> context = new ConcurrentHashMap<>();

    public SimpleTcpClient() {
        this(new TcpConfiguration());
    }

    public SimpleTcpClient(TcpConfiguration config) {
        client = new AsynchronousTcpClient(config);
        this.config = config;
    }

    public SimpleTcpClient alpnUnsupported(Action0 alpnUnsupported) {
        this.alpnUnsupported = alpnUnsupported;
        return this;
    }

    public SimpleTcpClient alpnSelected(Action1<String> alpnSelected) {
        this.alpnSelected = alpnSelected;
        return this;
    }

    public SimpleTcpClient alpnProtocols(Func0<List<String>> alpnProtocols) {
        this.alpnProtocols = alpnProtocols;
        return this;
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

            public void failed(Throwable x) {
                failed.call(x);
            }
        };
        connect(host, port, promise);
    }

    public void connect(String host, int port, Promise<TcpConnection> promise) {
        start();
        int sessionId = client.connect(host, port);
        ClientContext ctx = new ClientContext();
        ctx.promise = promise;
        context.put(sessionId, ctx);
    }

    public class ClientContext {
        Promise<TcpConnection> promise;
        TcpConnection connection;
    }

    public abstract class AbstractHandler extends AbstractSimpleHandler {

        @Override
        public void failedOpeningSession(Integer sessionId, Throwable t) throws Throwable {
            try {
                ClientContext ctx = context.get(sessionId);
                if (ctx != null && ctx.promise != null) {
                    ctx.promise.failed(t);
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

                private SSLContext sslContext = config.getSslContextFactory().getSSLContext();

                @Override
                public void sessionOpened(Session session) throws Throwable {
                    final SSLEngine sslEngine = sslContext.createSSLEngine();
                    SSLSession sslSession = new SSLSession(sslContext, sslEngine, session, true, (ssl) -> {
                        Object o = session.getAttachment();
                        if (o != null && o instanceof SecureTcpConnectionImpl) {
                            SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
                            sessionOpen(session, c);
                        }
                    }, new ALPN.ClientProvider() {

                        @Override
                        public List<String> protocols() {
                            if (alpnProtocols != null) {
                                return alpnProtocols.call();
                            } else {
                                return null;
                            }
                        }

                        @Override
                        public void unsupported() {
                            if (alpnUnsupported != null) {
                                alpnUnsupported.call();
                            }
                        }

                        @Override
                        public void selected(String protocol) {
                            if (alpnSelected != null) {
                                alpnSelected.call(protocol);
                            }
                        }
                    });
                    SecureTcpConnectionImpl c = new SecureTcpConnectionImpl(session, sslSession);
                    session.attachObject(c);
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
            ClientContext ctx = context.get(session.getSessionId());
            if (ctx != null && ctx.promise != null) {
                ctx.promise.succeeded(c);
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
