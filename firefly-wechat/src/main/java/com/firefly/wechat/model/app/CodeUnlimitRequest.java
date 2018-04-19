package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class CodeUnlimitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String scene;
    private String page;
    private Integer width;
    private Boolean auto_color;
    private LineColor line_color;

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Boolean getAuto_color() {
        return auto_color;
    }

    public void setAuto_color(Boolean auto_color) {
        this.auto_color = auto_color;
    }

    public LineColor getLine_color() {
        return line_color;
    }

    public void setLine_color(LineColor line_color) {
        this.line_color = line_color;
    }

    @Override
    public String toString() {
        return "CodeUnlimitRequest{" +
                "scene='" + scene + '\'' +
                ", page='" + page + '\'' +
                ", width=" + width +
                ", auto_color=" + auto_color +
                ", line_color=" + line_color +
                '}';
    }
}
