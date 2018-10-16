package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class Watermark implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appid;
    private Long timestamp;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Watermark{" +
                "appid='" + appid + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
