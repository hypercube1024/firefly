package com.firefly.wechat.model.message;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Pengtao Qiu
 */
@JacksonXmlRootElement(localName = "xml")
public class SmallAppEnterSessionMessage extends EventMessage {

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "SessionFrom")
    private String sessionFrom;

    public String getSessionFrom() {
        return sessionFrom;
    }

    public void setSessionFrom(String sessionFrom) {
        this.sessionFrom = sessionFrom;
    }

    @Override
    public String toString() {
        return "SmallAppEnterSessionMessage{" +
                "sessionFrom='" + sessionFrom + '\'' +
                ", event='" + event + '\'' +
                ", eventKey='" + eventKey + '\'' +
                ", ticket='" + ticket + '\'' +
                ", url='" + url + '\'' +
                ", toUserName='" + toUserName + '\'' +
                ", fromUserName='" + fromUserName + '\'' +
                ", createTime=" + createTime +
                ", msgType='" + msgType + '\'' +
                ", msgId=" + msgId +
                '}';
    }
}
