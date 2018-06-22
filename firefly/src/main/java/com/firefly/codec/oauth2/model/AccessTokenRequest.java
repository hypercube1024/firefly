package com.firefly.codec.oauth2.model;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String grantType;
    protected String code;
    protected String redirectUri;
    protected String clientId;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
