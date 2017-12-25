package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.tcp.codec.ffsocks.protocol.ControlFrame;
import com.firefly.net.tcp.codec.ffsocks.protocol.DataFrame;
import com.firefly.utils.concurrent.Callback;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface Stream extends ContextAttribute {

    int getId();

    CompletableFuture<Boolean> send(ControlFrame controlFrame);

    CompletableFuture<Boolean> send(DataFrame dataFrame);

    void send(ControlFrame controlFrame, Callback callback);

    void send(DataFrame dataFrame, Callback callback);

    void setListener(Listener listener);

    State getState();

    Session getSession();

    boolean isCommitted();

    interface Listener {
        void onControl(ControlFrame controlFrame);

        void onData(DataFrame dataFrame);
    }

    enum State {
        OPEN, LOCALLY_CLOSED, REMOTELY_CLOSED, CLOSED
    }

}
