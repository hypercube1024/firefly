package com.firefly.wechat.model.user;

import java.io.Serializable;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class UserData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> openid;
    private String next_openid;

    public List<String> getOpenid() {
        return openid;
    }

    public void setOpenid(List<String> openid) {
        this.openid = openid;
    }

    public String getNext_openid() {
        return next_openid;
    }

    public void setNext_openid(String next_openid) {
        this.next_openid = next_openid;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "openid=" + openid +
                ", next_openid='" + next_openid + '\'' +
                '}';
    }
}
