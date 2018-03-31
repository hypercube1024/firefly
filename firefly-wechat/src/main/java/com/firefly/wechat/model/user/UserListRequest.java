package com.firefly.wechat.model.user;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class UserListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String access_token;
    private String next_openid;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getNext_openid() {
        return next_openid;
    }

    public void setNext_openid(String next_openid) {
        this.next_openid = next_openid;
    }

    @Override
    public String toString() {
        return "UserListRequest{" +
                "access_token='" + access_token + '\'' +
                ", next_openid='" + next_openid + '\'' +
                '}';
    }
}
