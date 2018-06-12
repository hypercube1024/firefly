package com.firefly.wechat.model.user;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class UserId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String openid;
    private String lang; // zh_CN 简体，zh_TW 繁体，en 英语，默认为zh-CN

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
