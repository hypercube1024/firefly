package com.firefly.wechat.model.user;

import java.io.Serializable;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class BatchUserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<UserInfo> user_info_list;

    public List<UserInfo> getUser_info_list() {
        return user_info_list;
    }

    public void setUser_info_list(List<UserInfo> user_info_list) {
        this.user_info_list = user_info_list;
    }

    @Override
    public String toString() {
        return "BatchUserInfoResponse{" +
                "user_info_list=" + user_info_list +
                '}';
    }
}
