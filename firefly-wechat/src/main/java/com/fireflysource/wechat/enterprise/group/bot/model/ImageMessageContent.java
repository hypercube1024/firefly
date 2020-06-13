package com.fireflysource.wechat.enterprise.group.bot.model;

import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
public class ImageMessageContent {

    private String base64;
    private String md5;

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageMessageContent that = (ImageMessageContent) o;
        return base64.equals(that.base64) &&
                md5.equals(that.md5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base64, md5);
    }
}
