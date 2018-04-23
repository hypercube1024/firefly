package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ImageMessageContent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String media_id;

    public String getMedia_id() {
        return media_id;
    }

    public void setMedia_id(String media_id) {
        this.media_id = media_id;
    }

    @Override
    public String toString() {
        return "ImageMessageContent{" +
                "media_id='" + media_id + '\'' +
                '}';
    }
}
