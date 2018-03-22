package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appid;
    private String secret;
    private String code;
    private String grant_type = "authorization_code";

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    @Override
    public String toString() {
        return "AccessTokenRequest{" +
                "appid='" + appid + '\'' +
                ", secret='" + secret + '\'' +
                ", code='" + code + '\'' +
                ", grant_type='" + grant_type + '\'' +
                '}';
    }
}
