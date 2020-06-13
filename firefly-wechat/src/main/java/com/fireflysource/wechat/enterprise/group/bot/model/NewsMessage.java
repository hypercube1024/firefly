package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class NewsMessage extends Message {

    private NewsMessageContent news;

    public NewsMessage() {
        setMessageType(MessageType.NEWS);
    }

    public NewsMessageContent getNews() {
        return news;
    }

    public void setNews(NewsMessageContent news) {
        this.news = news;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NewsMessage that = (NewsMessage) o;
        return news.equals(that.news);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), news);
    }
}
