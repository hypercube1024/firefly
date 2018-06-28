package com.firefly.codec.oauth2.model;

import com.firefly.codec.oauth2.model.message.types.GrantType;
import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class RefreshingTokenRequest extends AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("client_id")
    protected String clientId;

    @JsonProperty("refresh_token")
    protected String refreshToken;

    protected String scope;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public static Builder newInstance() {
        return new RefreshingTokenRequest().new Builder();
    }

    public class Builder extends AbstractOauthBuilder<Builder, RefreshingTokenRequest> {

        public Builder() {
            builderInstance = this;
            object = RefreshingTokenRequest.this;
            object.grantType = GrantType.REFRESH_TOKEN.toString();
        }

        public Builder refreshToken(String refreshToken) {
            object.refreshToken = refreshToken;
            return this;
        }

        public Builder clientId(String clientId) {
            object.clientId = clientId;
            return this;
        }

        public Builder scope(String scope) {
            object.scope = scope;
            return this;
        }

    }
}
