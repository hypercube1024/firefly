package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class TextMessage extends Message {

    private TextMessageContent text;

    public TextMessage() {
        setMessageType(MessageType.TEXT);
    }

    public TextMessageContent getText() {
        return text;
    }

    public void setText(TextMessageContent text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TextMessage that = (TextMessage) o;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text);
    }
}
