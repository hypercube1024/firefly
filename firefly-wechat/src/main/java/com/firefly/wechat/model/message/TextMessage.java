package com.firefly.wechat.model.message;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Pengtao Qiu
 */
@JacksonXmlRootElement(localName = "xml")
public class TextMessage extends CommonMessage {

    @JacksonXmlProperty(localName = "Content")
    private String content;

    @Override
    public String toString() {
        return "TextMessage{" +
                "url='" + url + '\'' +
                ", toUserName='" + toUserName + '\'' +
                ", fromUserName='" + fromUserName + '\'' +
                ", createTime=" + createTime +
                ", msgType='" + msgType + '\'' +
                ", content='" + content + '\'' +
                ", msgId=" + msgId +
                '}';
    }
}
