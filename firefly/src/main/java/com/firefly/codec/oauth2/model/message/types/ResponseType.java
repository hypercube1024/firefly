package com.firefly.codec.oauth2.model.message.types;

import java.util.Arrays;

public enum ResponseType {

    CODE("code"), TOKEN("token");

    private String code;

    ResponseType(String code) {
        this.code = code;
    }

    public static ResponseType from(String responseType) {
        return Arrays.stream(ResponseType.values())
                     .filter(i -> i.toString().equals(responseType))
                     .findAny()
                     .orElse(null);
    }

    @Override
    public String toString() {
        return code;
    }
}
