package test.utils.json;

import com.firefly.utils.json.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class Token {

    @JsonProperty("access_token")
    private String accessToken;
    private Long expiresIn;
    private String scope;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @JsonProperty("expires_in")
    public Long getExpiresIn() {
        return expiresIn;
    }

    @JsonProperty("expires_in")
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(accessToken, token.accessToken) &&
                Objects.equals(expiresIn, token.expiresIn) &&
                Objects.equals(scope, token.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, expiresIn, scope);
    }
}
