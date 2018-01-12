package com.firefly.net.tcp.codec.common.decode;

import com.firefly.net.tcp.codec.AbstractMessageHandler;

public class DelimiterParser extends AbstractMessageHandler<String, String> {

    private StringBuilder buffer = new StringBuilder();
    private String delimiter;

    public DelimiterParser(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public void parse(String s) {
        try {
            buffer.append(s);
            int cursor = 0;
            int start;
            while ((start = buffer.indexOf(delimiter, cursor)) != -1) {
                action.call(buffer.substring(cursor, start));
                cursor = start + delimiter.length();
            }
            if (cursor < s.length()) {
                String remain = s.substring(cursor, s.length());
                buffer.delete(0, buffer.length());
                buffer.append(remain);
            } else {
                buffer.delete(0, buffer.length());
            }
        } catch (Throwable t) {
            exception.call(t);
        }
    }

}
