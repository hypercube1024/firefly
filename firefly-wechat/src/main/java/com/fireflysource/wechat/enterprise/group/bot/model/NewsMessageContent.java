package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.List;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class NewsMessageContent {

    private List<Article> articles;

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsMessageContent that = (NewsMessageContent) o;
        return Objects.equals(articles, that.articles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articles);
    }
}
