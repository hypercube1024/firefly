package com.firefly.net.tcp.codec;

import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class StringParser extends AbstractMessageHandler<ByteBuffer, String> {

    private final CharParser charParser = new CharParser();
    private final DelimiterParser delimiterParser;

    public StringParser() {
        this("\n");
    }

    public StringParser(String delimiter) {
        delimiterParser = new DelimiterParser(delimiter);
        charParser.complete(delimiterParser::receive);
    }

    @Override
    public void parse(ByteBuffer buffer) {
        charParser.receive(buffer);
    }

    @Override
    public StringParser complete(Action1<String> action) {
        super.complete(action);
        delimiterParser.complete(action);
        return this;
    }

    @Override
    public StringParser exception(Action1<Throwable> exception) {
        super.exception(exception);
        charParser.exception(exception);
        delimiterParser.exception(exception);
        return this;
    }

}
