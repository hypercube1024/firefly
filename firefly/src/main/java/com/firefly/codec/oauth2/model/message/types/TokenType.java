package com.firefly.codec.oauth2.model.message.types;

public enum TokenType {
    BEARER("Bearer"), MAC("MAC");

    private String tokenType;

    TokenType(String grantType) {
        this.tokenType = grantType;
    }

    @Override
    public String toString() {
        return tokenType;
    }
}
