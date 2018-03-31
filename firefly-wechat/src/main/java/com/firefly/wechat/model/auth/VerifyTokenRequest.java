package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class VerifyTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String access_token;
    private String openid;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    @Override
    public String toString() {
        return "VerifyTokenRequest{" +
                "access_token='" + access_token + '\'' +
                ", openid='" + openid + '\'' +
                '}';
    }
}
