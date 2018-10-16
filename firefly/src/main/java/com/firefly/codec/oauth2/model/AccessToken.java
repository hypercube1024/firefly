package com.firefly.codec.oauth2.model;

import com.firefly.codec.oauth2.model.message.types.TokenType;
import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Pengtao Qiu
 */
public class AccessToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("access_token")
    protected String accessToken;

    @JsonProperty("token_type")
    protected String tokenType = TokenType.BEARER.toString();

    @JsonProperty("expires_in")
    protected Long expiresIn;

    @JsonProperty("refresh_token")
    protected String refreshToken;

    protected String scope;

    protected String state;

    @JsonProperty("create_time")
    protected Date createTime;

    @JsonProperty("client_id")
    protected String clientId;

    protected String code;

    @JsonProperty("redirect_uri")
    protected String redirectUri;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

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
