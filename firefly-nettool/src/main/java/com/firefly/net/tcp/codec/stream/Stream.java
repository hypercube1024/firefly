package com.firefly.net.tcp.codec.stream;

import com.firefly.net.tcp.codec.protocol.ControlFrame;
import com.firefly.net.tcp.codec.protocol.DataFrame;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface Stream {

    int getId();

    CompletableFuture<Boolean> send(ControlFrame controlFrame);

    CompletableFuture<Boolean> send(DataFrame dataFrame);

    void setListener(Listener listener);

    Map<String, Object> getAttibutes();

    void setAttribute(String key, Object value);

    Object getAttribute(String key);

    State getState();

    interface Listener {

        void onControl(ControlFrame controlFrame);

        void onData(DataFrame dataFrame);

    }

    enum State {
        OPEN, LOCALLY_CLOSED, REMOTELY_CLOSED, CLOSED
    }

}
