package com.firefly.wechat.model.message;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Pengtao Qiu
 */
@JacksonXmlRootElement(localName = "xml")
public class CommonMessage {

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "URL")
    protected String url;

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "ToUserName")
    protected String toUserName;

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "FromUserName")
    protected String fromUserName;

    @JacksonXmlProperty(localName = "CreateTime")
    protected Long createTime;

    @JacksonXmlProperty(localName = "MsgType")
    protected String msgType;

    @JacksonXmlProperty(localName = "MsgId")
    protected Long msgId;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Long getMsgId() {
        return msgId;
    }

    public void setMsgId(Long msgId) {
        this.msgId = msgId;
    }
}
