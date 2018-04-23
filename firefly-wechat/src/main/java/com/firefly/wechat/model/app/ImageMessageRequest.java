package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class ImageMessageRequest extends CommonMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private ImageMessageContent image;

    public ImageMessageRequest() {
        msgtype = "image";
    }

    public ImageMessageContent getImage() {
        return image;
    }

    public void setImage(ImageMessageContent image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "ImageMessageRequest{" +
                "image=" + image +
                ", touser='" + touser + '\'' +
                ", msgtype='" + msgtype + '\'' +
                '}';
    }
}
