package com.firefly.net.tcp;

import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Func1;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.eclipse.jetty.alpn.ALPN;

import javax.net.ssl.SSLEngine;
import java.util.List;

public class SimpleTcpServer extends AbstractLifeCycle {

    // accept TCP connection callback
    private Action1<TcpConnection> accept;
    private Action2<Integer, Throwable> failedAcceptance;

    // ALPN callback
    private Action0 alpnUnsupported;
    private Func1<List<String>, String> alpnSelectProtocols;

    private AsynchronousTcpServer server;
    private TcpServerConfiguration config;

    public SimpleTcpServer() {
        this(new TcpServerConfiguration());
    }

    public SimpleTcpServer(TcpServerConfiguration config) {
        this.config = config;
        server = new AsynchronousTcpServer(config);
    }

    public SimpleTcpServer accept(Action1<TcpConnection> accept) {
        this.accept = accept;
        return this;
    }

    public SimpleTcpServer accept(Action1<TcpConnection> accept, Action2<Integer, Throwable> failed) {
        this.accept = accept;
        this.failedAcceptance = failed;
        return this;
    }

    public SimpleTcpServer alpnUnsupported(Action0 alpnUnsupported) {
        this.alpnUnsupported = alpnUnsupported;
        return this;
    }

    public SimpleTcpServer alpnSelectProtocols(Func1<List<String>, String> alpnSelectProtocols) {
        this.alpnSelectProtocols = alpnSelectProtocols;
        return this;
    }

    public void listen(String host, int port) {
        config.setHost(host);
        config.setPort(port);
        start();
    }

    public abstract class AbstractHandler extends AbstractSimpleHandler {

        @Override
        public void failedAcceptingSession(Integer sessionId, Throwable t) throws Throwable {
            if (failedAcceptance != null) {
                failedAcceptance.call(sessionId, t);
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
                    if (accept != null) {
                        accept.call(c);
                    }
                }

            });
        } else {
            config.setDecoder(AbstractSimpleHandler.sslDecoder);
            config.setHandler(new AbstractHandler() {

                @Override
                public void sessionOpened(Session session) throws Throwable {

                    final SSLEngine sslEngine = config.getSslContextFactory().createSSLEngine(false);
                    SSLSession sslSession = new SSLSession(sslEngine, session, (ssl) -> {
                        Object o = session.getAttachment();
                        if (o != null && o instanceof SecureTcpConnectionImpl) {
                            SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
                            if (accept != null) {
                                accept.call(c);
                            }
                        }
                    }, new ALPN.ServerProvider() {

                        @Override
                        public void unsupported() {
                            try {
                                alpnUnsupported.call();
                            } finally {
                                ALPN.remove(sslEngine);
                            }
                        }

                        @Override
                        public String select(List<String> protocols) {
                            try {
                                return alpnSelectProtocols.call(protocols);
                            } finally {
                                ALPN.remove(sslEngine);
                            }
                        }
                    });
                    SecureTcpConnectionImpl c = new SecureTcpConnectionImpl(session, sslSession);
                    session.attachObject(c);
                }

                @Override
                public void sessionClosed(Session session) throws Throwable {
                    super.sslSessionClosed(session);
                }

            });
        }
        server.listen(config.getHost(), config.getPort());
    }

    @Override
    protected void destroy() {
        server.stop();
    }

}
