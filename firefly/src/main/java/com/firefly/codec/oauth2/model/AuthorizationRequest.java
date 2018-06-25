package com.firefly.codec.oauth2.model;

import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static com.firefly.codec.oauth2.model.OAuth.*;

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

    public static Builder newInstance() {
        return new AuthorizationRequest().new Builder();
    }

    public class Builder extends AbstractOauthBuilder<Builder, AuthorizationRequest> {

        public Builder() {
            this.instance = this;
            this.object = AuthorizationRequest.this;
        }

        public Builder responseType(String responseType) {
            AuthorizationRequest.this.responseType = responseType;
            return this;
        }

        public Builder clientId(String clientId) {
            AuthorizationRequest.this.clientId = clientId;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            AuthorizationRequest.this.redirectUri = redirectUri;
            return this;
        }

        public Builder scope(String scope) {
            AuthorizationRequest.this.scope = scope;
            return this;
        }

        public Builder state(String state) {
            AuthorizationRequest.this.state = state;
            return this;
        }

        @Override
        public String toEncodedUrl() {
            urlEncoded.put(OAUTH_RESPONSE_TYPE, responseType);
            urlEncoded.put(OAUTH_CLIENT_ID, clientId);
            urlEncoded.put(OAUTH_REDIRECT_URI, redirectUri);
            urlEncoded.put(OAUTH_SCOPE, scope);
            urlEncoded.put(OAUTH_STATE, state);
            return urlEncoded.encode(StandardCharsets.UTF_8, true);
        }
    }
}
