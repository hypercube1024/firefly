package com.fireflysource.common.actor;

import com.fireflysource.common.sys.Result;

import java.util.concurrent.CompletableFuture;

abstract public class AbstractAsyncActor<T> extends AbstractActor<T> {

    public AbstractAsyncActor() {
        super();
    }

    public AbstractAsyncActor(String address, Dispatcher dispatcher, Mailbox<T, SystemMessage> mailbox) {
        super(address, dispatcher, mailbox);
    }

    @Override
    public void onReceive(T message) {
        pause();
        onReceiveAsync(message).handle((result, throwable) -> {
            resume();
            return Result.DONE;
        });
    }

    abstract public CompletableFuture<Void> onReceiveAsync(T message);
}
