package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class MessageBuilder {

    public static TextMessageBuilder text() {
        return new TextMessageBuilder();
    }

    public static MarkdownMessageBuilder markdown() {
        return new MarkdownMessageBuilder();
    }

    public static ImageMessageBuilder image() {
        return new ImageMessageBuilder();
    }

    public static NewsMessageBuilder news() {
        return new NewsMessageBuilder();
    }

    public static class TextMessageBuilder {

        private TextMessageContent content = new TextMessageContent();

        public TextMessageBuilder content(String content) {
            this.content.setContent(content);
            return this;
        }

        public TextMessageBuilder mentionedList(List<String> mentionedList) {
            content.setMentionedList(mentionedList);
            return this;
        }

        public TextMessageBuilder mentionedMobileList(List<String> mentionedMobileList) {
            content.setMentionedMobileList(mentionedMobileList);
            return this;
        }

        public TextMessage end() {
            TextMessage textMessage = new TextMessage();
            textMessage.setText(content);
            return textMessage;
        }
    }

    public static class MarkdownMessageBuilder {
        private MarkdownMessageContent content = new MarkdownMessageContent();

        public MarkdownMessageBuilder content(String content) {
            this.content.setContent(content);
            return this;
        }

        public MarkdownMessage end() {
            MarkdownMessage markdownMessage = new MarkdownMessage();
            markdownMessage.setMarkdown(content);
            return markdownMessage;
        }
    }

    public static class ImageMessageBuilder {
        private ImageMessageContent content = new ImageMessageContent();

        public ImageMessageBuilder md5(String md5) {
            content.setMd5(md5);
            return this;
        }

        public ImageMessageBuilder base64(String base64) {
            content.setBase64(base64);
            return this;
        }

        public ImageMessage end() {
            ImageMessage imageMessage = new ImageMessage();
            imageMessage.setImage(content);
            return imageMessage;
        }
    }

    public static class NewsMessageBuilder {

        private NewsMessageContent content = new NewsMessageContent();

        public NewsMessageBuilder() {
            content.setArticles(new LinkedList<>());
        }

        public NewsMessageBuilder addArticle(Article article) {
            content.getArticles().add(article);
            return this;
        }

        public NewsMessageBuilder addArticles(List<Article> articles) {
            content.getArticles().addAll(articles);
            return this;
        }

        public NewsMessage end() {
            NewsMessage newsMessage = new NewsMessage();
            newsMessage.setNews(content);
            return newsMessage;
        }
    }
}
