package com.firefly.wechat.model.template;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class TemplateMessageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer errcode;
    private String errmsg;
    private Long msgid;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public Long getMsgid() {
        return msgid;
    }

    public void setMsgid(Long msgid) {
        this.msgid = msgid;
    }

    @Override
    public String toString() {
        return "TemplateMessageResponse{" +
                "errcode=" + errcode +
                ", errmsg='" + errmsg + '\'' +
                ", msgid=" + msgid +
                '}';
    }
}
