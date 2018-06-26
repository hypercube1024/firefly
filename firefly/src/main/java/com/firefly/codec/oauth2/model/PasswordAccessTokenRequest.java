package com.firefly.codec.oauth2.model;

import com.firefly.codec.oauth2.model.message.types.GrantType;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static com.firefly.codec.oauth2.model.OAuth.*;

public class PasswordAccessTokenRequest extends AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String username;
    protected String password;
    protected String scope;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public static Builder newInstance() {
        return new PasswordAccessTokenRequest().new Builder();
    }

    public class Builder extends AbstractOauthBuilder<Builder, PasswordAccessTokenRequest> {

        public Builder() {
            builderInstance = this;
            object = PasswordAccessTokenRequest.this;
            object.grantType = GrantType.PASSWORD.toString();
        }

        public Builder username(String username) {
            object.username = username;
            return this;
        }

        public Builder password(String password) {
            object.password = password;
            return this;
        }

        public Builder scope(String scope) {
            object.scope = scope;
            return this;
        }

        @Override
        public String toEncodedUrl() {
            urlEncoded.put(OAUTH_GRANT_TYPE, grantType);
            urlEncoded.put(OAUTH_USERNAME, username);
            urlEncoded.put(OAUTH_PASSWORD, password);
            urlEncoded.put(OAUTH_SCOPE, scope);
            return urlEncoded.encode(StandardCharsets.UTF_8, true);
        }
    }
}
