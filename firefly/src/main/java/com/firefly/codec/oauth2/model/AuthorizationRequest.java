package com.firefly.codec.oauth2.model;

import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AuthorizationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("response_type")
    protected String responseType;
    @JsonProperty("client_id")
    protected String clientId;
    @JsonProperty("redirect_uri")
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
