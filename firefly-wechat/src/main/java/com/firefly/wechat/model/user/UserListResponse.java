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

    @Override
    public String toString() {
        return "UserListResponse{" +
                "total=" + total +
                ", count=" + count +
                ", data=" + data +
                '}';
    }
}
