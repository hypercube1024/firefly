package com.firefly.codec.oauth2.model;

import com.firefly.codec.oauth2.model.message.types.GrantType;
import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static com.firefly.codec.oauth2.model.OAuth.*;

/**
 * @author Pengtao Qiu
 */
public class ClientCredentialAccessTokenRequest extends AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("client_id")
    protected String clientId;

    @JsonProperty("client_secret")
    protected String clientSecret;

    protected String scope;

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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public static Builder newInstance() {
        return new ClientCredentialAccessTokenRequest().new Builder();
    }

    public class Builder extends AbstractOauthBuilder<Builder, ClientCredentialAccessTokenRequest> {

        public Builder() {
            builderInstance = this;
            object = ClientCredentialAccessTokenRequest.this;
            object.grantType = GrantType.CLIENT_CREDENTIALS.toString();
        }

        public Builder clientId(String clientId) {
            object.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            object.clientSecret = clientSecret;
            return this;
        }

        public Builder scope(String scope) {
            object.scope = scope;
            return this;
        }

    }
}
