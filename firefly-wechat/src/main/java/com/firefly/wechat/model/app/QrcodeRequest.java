package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class QrcodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String path;
    private Integer width;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "QrcodeRequest{" +
                "path='" + path + '\'' +
                ", width=" + width +
                '}';
    }
}
