package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class CommonMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String touser;
    protected String msgtype;

    public String getTouser() {
        return touser;
    }

    public void setTouser(String touser) {
        this.touser = touser;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    @Override
    public String toString() {
        return "CommonMessageRequest{" +
                "touser='" + touser + '\'' +
                ", msgtype='" + msgtype + '\'' +
                '}';
    }
}
