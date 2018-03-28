package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class SessionKeyResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String openid;
    private String session_key;

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getSession_key() {
        return session_key;
    }

    public void setSession_key(String session_key) {
        this.session_key = session_key;
    }

    @Override
    public String toString() {
        return "SessionKeyResponse{" +
                "openid='" + openid + '\'' +
                ", session_key='" + session_key + '\'' +
                '}';
    }
}
