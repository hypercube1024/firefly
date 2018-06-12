package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class PageMessageContent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String pagepath;
    private String thumb_media_id;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPagepath() {
        return pagepath;
    }

    public void setPagepath(String pagepath) {
        this.pagepath = pagepath;
    }

    public String getThumb_media_id() {
        return thumb_media_id;
    }

    public void setThumb_media_id(String thumb_media_id) {
        this.thumb_media_id = thumb_media_id;
    }

    @Override
    public String toString() {
        return "PageMessageContent{" +
                "title='" + title + '\'' +
                ", pagepath='" + pagepath + '\'' +
                ", thumb_media_id='" + thumb_media_id + '\'' +
                '}';
    }
}
