package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class LinkMessageRequest extends CommonMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private LinkMessageContent link;

    public LinkMessageRequest() {
        msgtype = "link";
    }

    public LinkMessageContent getLink() {
        return link;
    }

    public void setLink(LinkMessageContent link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "LinkMessageRequest{" +
                "link=" + link +
                ", touser='" + touser + '\'' +
                ", msgtype='" + msgtype + '\'' +
                '}';
    }
}
