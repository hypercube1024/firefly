package com.firefly.codec.oauth2.model;

import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ClientCredentialAccessTokenRequest extends AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("client_id")
    protected String clientId;
    @JsonProperty("client_secret")
    protected String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
