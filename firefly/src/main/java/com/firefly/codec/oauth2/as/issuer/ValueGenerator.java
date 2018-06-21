package com.firefly.codec.oauth2.as.issuer;

import com.firefly.codec.oauth2.exception.OAuthSystemException;

public interface ValueGenerator {

    String generateValue() throws OAuthSystemException;

    String generateValue(String param) throws OAuthSystemException;
}
