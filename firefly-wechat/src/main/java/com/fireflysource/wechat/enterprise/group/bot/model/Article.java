package com.fireflysource.wechat.enterprise.group.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class Article {

    private String title;

    private String description;

    private String url;

    @JsonProperty("picurl")
    private String pictureUrl;

    public Article() {
    }

    public Article(String title, String description, String url, String pictureUrl) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.pictureUrl = pictureUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equals(title, article.title) &&
                Objects.equals(description, article.description) &&
                Objects.equals(url, article.url) &&
                Objects.equals(pictureUrl, article.pictureUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, url, pictureUrl);
    }
}
