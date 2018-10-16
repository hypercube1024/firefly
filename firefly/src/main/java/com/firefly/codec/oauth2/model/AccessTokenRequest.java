package com.firefly.codec.oauth2.model;

import com.firefly.utils.json.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class AccessTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("grant_type")
    protected String grantType;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

}
