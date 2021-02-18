package com.fireflysource.wechat.enterprise.group.bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class Message {

    @JsonProperty("msgtype")
    private MessageType messageType;

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return messageType == message.messageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType);
    }
}
