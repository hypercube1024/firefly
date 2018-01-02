package com.firefly.client.http2;

import com.firefly.codec.http2.frame.GoAwayFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class ClientHTTP2SessionListener extends Session.Listener.Adapter {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private HTTP2ClientConnection connection;

    public ClientHTTP2SessionListener() {
    }

    public ClientHTTP2SessionListener(HTTP2ClientConnection connection) {
        this.connection = connection;
    }

    public HTTP2ClientConnection getConnection() {
        return connection;
    }

    public void setConnection(HTTP2ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public void onClose(Session session, GoAwayFrame frame) {
        log.warn("Client received the GoAwayFrame -> {}", frame.toString());
        Optional.ofNullable(connection).ifPresent(IO::close);
    }

    @Override
    public void onFailure(Session session, Throwable failure) {
        log.error("Client failure: " + session, failure);
        Optional.ofNullable(connection).ifPresent(IO::close);
    }

    @Override
    public void onReset(Session session, ResetFrame frame) {
        log.warn("Client received ResetFrame {}", frame.toString());
        Optional.ofNullable(connection).ifPresent(IO::close);
    }
}
