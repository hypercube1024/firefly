package com.firefly.codec.oauth2.as.issuer;

import java.util.UUID;


public class UUIDValueGenerator implements ValueGenerator {

    @Override
    public String generateValue() {
        return generateValue(UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String generateValue(String param) {
        return UUID.fromString(UUID.nameUUIDFromBytes(param.getBytes()).toString()).toString().replace("-", "");
    }
}
