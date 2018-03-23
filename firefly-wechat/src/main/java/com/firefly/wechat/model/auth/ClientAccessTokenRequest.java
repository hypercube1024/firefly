package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ClientAccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String grant_type = "client_credential";
    private String appid;
    private String secret;

    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return "ClientAccessTokenRequest{" +
                "grant_type='" + grant_type + '\'' +
                ", appid='" + appid + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
