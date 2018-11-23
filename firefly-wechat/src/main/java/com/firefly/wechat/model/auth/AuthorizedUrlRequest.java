package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AuthorizedUrlRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SCOPE_BASE = "snsapi_base";
    public static final String SCOPE_USERINFO = "snsapi_userinfo";

    protected String appid;
    protected String redirectUri;
    protected String scope; // snsapi_base or snsapi_userinfo
    protected String state;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
