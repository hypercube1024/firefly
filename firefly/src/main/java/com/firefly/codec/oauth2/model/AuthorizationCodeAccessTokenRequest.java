package com.firefly.codec.oauth2.model;

import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;

public class AuthorizationCodeAccessTokenRequest extends AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("client_id")
    protected String clientId;
    protected String code;
    @JsonProperty("redirect_uri")
    protected String redirectUri;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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
}
