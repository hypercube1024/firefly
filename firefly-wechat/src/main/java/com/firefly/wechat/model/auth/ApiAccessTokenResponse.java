package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ApiAccessTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String access_token;
    private Integer expires_in;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public Integer getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Integer expires_in) {
        this.expires_in = expires_in;
    }

    @Override
    public String toString() {
        return "ApiAccessTokenResponse{" +
                "access_token='" + access_token + '\'' +
                ", expires_in=" + expires_in +
                '}';
    }
}
