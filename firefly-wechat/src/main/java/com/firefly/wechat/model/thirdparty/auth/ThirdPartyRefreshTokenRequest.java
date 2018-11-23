package com.firefly.wechat.model.thirdparty.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ThirdPartyRefreshTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appid;
    private String grant_type = "refresh_token";
    private String refresh_token;
    private String component_appid;
    private String component_access_token;

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

    public String getComponent_appid() {
        return component_appid;
    }

    public void setComponent_appid(String component_appid) {
        this.component_appid = component_appid;
    }

    public String getComponent_access_token() {
        return component_access_token;
    }

    public void setComponent_access_token(String component_access_token) {
        this.component_access_token = component_access_token;
    }

    @Override
    public String toString() {
        return "ThirdPartyRefreshTokenRequest{" +
                "appid='" + appid + '\'' +
                ", grant_type='" + grant_type + '\'' +
                ", refresh_token='" + refresh_token + '\'' +
                ", component_appid='" + component_appid + '\'' +
                ", component_access_token='" + component_access_token + '\'' +
                '}';
    }
}
