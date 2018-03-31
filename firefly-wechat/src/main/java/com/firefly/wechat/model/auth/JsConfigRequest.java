package com.firefly.wechat.model.auth;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class JsConfigRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ticket;
    private String url;
    private String appId;

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String toString() {
        return "JsConfigRequest{" +
                "ticket='" + ticket + '\'' +
                ", url='" + url + '\'' +
                ", appId='" + appId + '\'' +
                '}';
    }
}
