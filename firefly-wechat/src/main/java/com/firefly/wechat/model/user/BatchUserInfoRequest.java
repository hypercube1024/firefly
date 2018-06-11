package com.firefly.wechat.model.user;

import java.io.Serializable;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class BatchUserInfoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<UserId> user_list;

    public List<UserId> getUser_list() {
        return user_list;
    }

    public void setUser_list(List<UserId> user_list) {
        this.user_list = user_list;
    }

    @Override
    public String toString() {
        return "BatchUserInfoRequest{" +
                "user_list=" + user_list +
                '}';
    }
}
