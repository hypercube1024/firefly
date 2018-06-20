package com.firefly.codec.oauth2.model.message.types;

public enum ResponseType {

    CODE("code"), TOKEN("token");

    private String code;

    ResponseType(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
