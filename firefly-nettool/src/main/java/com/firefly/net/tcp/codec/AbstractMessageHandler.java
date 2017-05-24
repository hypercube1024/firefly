package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractMessageHandler<R, T> implements MessageHandler<R, T>{

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    protected Action1<T> action;
    protected Action1<Throwable> exception;

    @Override
    public void receive(R obj) {
        check();
        parse(obj);
    }

    @Override
    public MessageHandler<R, T> complete(Action1<T> action) {
        this.action = action;
        return this;
    }

    @Override
    public MessageHandler<R, T> exception(Action1<Throwable> exception) {
        this.exception = exception;
        return this;
    }

    abstract protected void parse(R obj);

    private void check() {
        if (this.action == null) {
            throw new IllegalArgumentException("the complete callback is null");
        }
        if (this.exception == null) {
            exception = (t) -> log.error("parsing exception", t);
        }
    }
}
