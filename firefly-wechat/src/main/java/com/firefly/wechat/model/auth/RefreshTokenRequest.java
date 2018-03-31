package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class RefreshTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appid;
    private String grant_type = "refresh_token";
    private String refresh_token;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{" +
                "appid='" + appid + '\'' +
                ", grant_type='" + grant_type + '\'' +
                ", refresh_token='" + refresh_token + '\'' +
                '}';
    }
}
