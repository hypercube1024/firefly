package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class MarkdownMessage extends Message {

    private MarkdownMessageContent markdown;

    public MarkdownMessage() {
        setMessageType(MessageType.MARKDOWN);
    }

    public MarkdownMessageContent getMarkdown() {
        return markdown;
    }

    public void setMarkdown(MarkdownMessageContent markdown) {
        this.markdown = markdown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MarkdownMessage that = (MarkdownMessage) o;
        return markdown.equals(that.markdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), markdown);
    }
}
