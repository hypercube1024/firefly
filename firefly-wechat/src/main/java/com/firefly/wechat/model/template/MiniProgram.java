package com.firefly.wechat.model.template;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class MiniProgram implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appid;
    private String pagepath;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getPagepath() {
        return pagepath;
    }

    public void setPagepath(String pagepath) {
        this.pagepath = pagepath;
    }

    @Override
    public String toString() {
        return "MiniProgram{" +
                "appid='" + appid + '\'' +
                ", pagepath='" + pagepath + '\'' +
                '}';
    }
}
