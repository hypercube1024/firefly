package com.firefly.codec.oauth2.as.service;

import com.firefly.codec.oauth2.model.*;

public interface AuthorizationService {

    String generateCode(AuthorizationRequest request);

    AccessTokenResponse generateAccessToken(AuthorizationCodeAccessTokenRequest request);

    AccessTokenResponse generateAccessToken(AuthorizationRequest request);

    AccessTokenResponse generateAccessToken(PasswordAccessTokenRequest request);

    AccessTokenResponse generateAccessToken(ClientCredentialAccessTokenRequest request);

    AccessTokenResponse refreshToken(RefreshingTokenRequest request);

    void verifyAccessToken(String token);
}
