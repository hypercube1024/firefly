package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;

public interface MessageHandler<R, T> {

    void receive(R obj);

    MessageHandler<R, T> complete(Action1<T> action);

    MessageHandler<R, T> exception(Action1<Throwable> exception);

}
