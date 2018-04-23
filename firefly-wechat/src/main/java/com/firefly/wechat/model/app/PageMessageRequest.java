package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class PageMessageRequest extends CommonMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private PageMessageContent miniprogrampage;

    public PageMessageRequest() {
        msgtype = "miniprogrampage";
    }

    public PageMessageContent getMiniprogrampage() {
        return miniprogrampage;
    }

    public void setMiniprogrampage(PageMessageContent miniprogrampage) {
        this.miniprogrampage = miniprogrampage;
    }

    @Override
    public String toString() {
        return "PageMessageRequest{" +
                "miniprogrampage=" + miniprogrampage +
                ", touser='" + touser + '\'' +
                ", msgtype='" + msgtype + '\'' +
                '}';
    }
}
