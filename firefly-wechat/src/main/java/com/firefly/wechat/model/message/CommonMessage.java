package com.firefly.wechat.model.message;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Pengtao Qiu
 */
@JacksonXmlRootElement(localName = "xml")
public class CommonMessage {

    @JacksonXmlProperty(localName = "URL")
    protected String url;

    @JacksonXmlProperty(localName = "ToUserName")
    protected String toUserName;

    @JacksonXmlProperty(localName = "FromUserName")
    protected String fromUserName;

    @JacksonXmlProperty(localName = "CreateTime")
    protected Integer createTime;

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

    public Integer getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Integer createTime) {
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
