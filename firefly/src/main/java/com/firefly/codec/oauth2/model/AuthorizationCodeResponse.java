package com.firefly.codec.oauth2.model;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AuthorizationCodeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String code;
    protected String state;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
