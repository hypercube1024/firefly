package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class ImageMessage extends Message {

    private ImageMessageContent image;

    public ImageMessage() {
        setMessageType(MessageType.IMAGE);
    }

    public ImageMessageContent getImage() {
        return image;
    }

    public void setImage(ImageMessageContent image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImageMessage that = (ImageMessage) o;
        return image.equals(that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), image);
    }
}
