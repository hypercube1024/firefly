package com.firefly.codec.oauth2.model;

import com.firefly.codec.oauth2.model.message.types.GrantType;
import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static com.firefly.codec.oauth2.model.OAuth.*;

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

    public static Builder newInstance() {
        return new AuthorizationCodeAccessTokenRequest().new Builder();
    }

    public class Builder extends AbstractOauthBuilder<Builder, AuthorizationCodeAccessTokenRequest> {

        public Builder() {
            builderInstance = this;
            object = AuthorizationCodeAccessTokenRequest.this;
            object.grantType = GrantType.AUTHORIZATION_CODE.toString();
        }

        public Builder code(String code) {
            object.code = code;
            return this;
        }

        public Builder clientId(String clientId) {
            object.clientId = clientId;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            object.redirectUri = redirectUri;
            return this;
        }

        @Override
        public String toEncodedUrl() {
            urlEncoded.put(OAUTH_GRANT_TYPE, grantType);
            urlEncoded.put(OAUTH_CODE, code);
            urlEncoded.put(OAUTH_CLIENT_ID, clientId);
            urlEncoded.put(OAUTH_REDIRECT_URI, redirectUri);
            return urlEncoded.encode(StandardCharsets.UTF_8, true);
        }

    }
}
