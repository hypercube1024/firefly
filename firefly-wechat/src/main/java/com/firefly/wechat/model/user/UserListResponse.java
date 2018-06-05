package com.firefly.wechat.model.user;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class UserListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer total;
    private Integer count;
    private UserData data;
    private String next_openid;

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public UserData getData() {
        return data;
    }

    public void setData(UserData data) {
        this.data = data;
    }

    public String getNext_openid() {
        return next_openid;
    }

    public void setNext_openid(String next_openid) {
        this.next_openid = next_openid;
    }

    @Override
    public String toString() {
        return "UserListResponse{" +
                "total=" + total +
                ", count=" + count +
                ", next_openid='" + next_openid + '\'' +
                '}';
    }
}
