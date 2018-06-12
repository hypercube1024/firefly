package com.firefly.wechat.model.user;

import java.io.Serializable;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class UserData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> openid;

    public List<String> getOpenid() {
        return openid;
    }

    public void setOpenid(List<String> openid) {
        this.openid = openid;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "openid=" + openid +
                '}';
    }
}
