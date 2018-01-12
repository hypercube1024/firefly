package com.firefly.codec.common;

import com.firefly.net.Connection;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ConnectionEvent<T extends Connection> {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final T connection;
    private final List<Action1<T>> closedListeners = new LinkedList<>();
    private final List<Action2<T, Throwable>> exceptionListeners = new LinkedList<>();

    public ConnectionEvent(T connection) {
        this.connection = connection;
    }

    public T onClose(Action1<T> closedListener) {
        closedListeners.add(closedListener);
        return connection;
    }

    public T onException(Action2<T, Throwable> exceptionListener) {
        exceptionListeners.add(exceptionListener);
        return connection;
    }

    public void notifyClose() {
        log.info("The handler called {} closed listener. Session: {}", this.getClass(), connection.getSessionId());
        closedListeners.forEach(c -> c.call(connection));
    }

    public void notifyException(Throwable t) {
        log.info("The handler called {} exception listener. Session: {}", this.getClass(), connection.getSessionId());
        exceptionListeners.forEach(e -> e.call(connection, t));
    }
}
