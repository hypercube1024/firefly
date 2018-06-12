package com.firefly.wechat.model.message;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Pengtao Qiu
 */
@JacksonXmlRootElement(localName = "xml")
public class EventMessage extends CommonMessage {

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "Event")
    protected String event;

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "EventKey")
    protected String eventKey;

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "Ticket")
    protected String ticket;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "event='" + event + '\'' +
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
