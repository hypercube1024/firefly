package com.firefly.codec.oauth2.as.issuer;

public class OAuthIssuerImpl implements OAuthIssuer {

    private ValueGenerator vg;

    public OAuthIssuerImpl(ValueGenerator vg) {
        this.vg = vg;
    }

    public String accessToken() {
        return vg.generateValue();
    }

    public String refreshToken() {
        return vg.generateValue();
    }

    public String authorizationCode() {
        return vg.generateValue();
    }
}
