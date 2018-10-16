package com.firefly.codec.oauth2.model.message.types;

import java.util.Arrays;

public enum GrantType {
    // NONE("none"),
    AUTHORIZATION_CODE("authorization_code"),
    IMPLICIT("implicit"),
    PASSWORD("password"),
    REFRESH_TOKEN("refresh_token"),
    CLIENT_CREDENTIALS("client_credentials"),
    JWT_BEARER("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private String grantType;

    GrantType(String grantType) {
        this.grantType = grantType;
    }

    public static GrantType from(String grantType) {
        return Arrays.stream(GrantType.values())
                     .filter(i -> i.toString().equals(grantType))
                     .findAny()
                     .orElse(null);
    }

    @Override
    public String toString() {
        return grantType;
    }
}
