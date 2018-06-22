package com.firefly.codec.oauth2.model;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AuthorizationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String responseType;
    protected String clientId;
    protected String redirectUri;
    protected String scope;
    protected String state;

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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
