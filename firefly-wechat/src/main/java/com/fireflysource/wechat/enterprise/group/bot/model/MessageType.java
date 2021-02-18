package com.fireflysource.wechat.enterprise.group.bot.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Pengtao Qiu
 */
public enum MessageType {

    TEXT("text"), MARKDOWN("markdown"), IMAGE("image"), NEWS("news");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
