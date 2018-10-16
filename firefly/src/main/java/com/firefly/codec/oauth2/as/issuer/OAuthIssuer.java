package com.firefly.codec.oauth2.as.issuer;

import com.firefly.codec.oauth2.exception.OAuthSystemException;

public interface OAuthIssuer {

    String accessToken() throws OAuthSystemException;

    String authorizationCode() throws OAuthSystemException;

    String refreshToken() throws OAuthSystemException;
}
