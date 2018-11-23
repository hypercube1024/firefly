package com.firefly.wechat.model.thirdparty.auth;

import com.firefly.wechat.model.auth.AuthorizedUrlRequest;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ThirdPartyAuthorizedUrlRequest extends AuthorizedUrlRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String componentAppid;

    public String getComponentAppid() {
        return componentAppid;
    }

    public void setComponentAppid(String componentAppid) {
        this.componentAppid = componentAppid;
    }

    @Override
    public String toString() {
        return "ThirdPartyAuthorizedUrlRequest{" +
                "componentAppid='" + componentAppid + '\'' +
                ", appid='" + appid + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", scope='" + scope + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
