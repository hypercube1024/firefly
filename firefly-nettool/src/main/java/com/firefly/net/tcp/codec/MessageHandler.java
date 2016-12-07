package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;

public interface MessageHandler<R, T> {

    void receive(R obj);

    void complete(Action1<T> complete);

}
