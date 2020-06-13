package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class MarkdownMessageContent {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarkdownMessageContent that = (MarkdownMessageContent) o;
        return content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
