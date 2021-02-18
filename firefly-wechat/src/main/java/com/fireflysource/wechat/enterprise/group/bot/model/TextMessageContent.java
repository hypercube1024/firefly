package com.fireflysource.wechat.enterprise.group.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class TextMessageContent {

    private String content;

    @JsonProperty("mentioned_list")
    private List<String> mentionedList;

    @JsonProperty("mentioned_mobile_list")
    private List<String> mentionedMobileList;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getMentionedList() {
        return mentionedList;
    }

    public void setMentionedList(List<String> mentionedList) {
        this.mentionedList = mentionedList;
    }

    public List<String> getMentionedMobileList() {
        return mentionedMobileList;
    }

    public void setMentionedMobileList(List<String> mentionedMobileList) {
        this.mentionedMobileList = mentionedMobileList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextMessageContent that = (TextMessageContent) o;
        return content.equals(that.content) &&
                Objects.equals(mentionedList, that.mentionedList) &&
                Objects.equals(mentionedMobileList, that.mentionedMobileList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, mentionedList, mentionedMobileList);
    }
}
