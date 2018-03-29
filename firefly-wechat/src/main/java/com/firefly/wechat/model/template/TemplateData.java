package com.firefly.wechat.model.template;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class TemplateData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private String color;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "TemplateData{" +
                "value='" + value + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
