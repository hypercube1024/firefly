package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class TextMessageRequest extends CommonMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private TextMessageContent text;

    public TextMessageRequest() {
        msgtype = "text";
    }

    public TextMessageContent getText() {
        return text;
    }

    public void setText(TextMessageContent text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TextMessageRequest{" +
                "text=" + text +
                ", touser='" + touser + '\'' +
                ", msgtype='" + msgtype + '\'' +
                '}';
    }
}
