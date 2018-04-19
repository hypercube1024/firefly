package com.firefly.wechat.model.app;

import java.io.Serializable;

/**
 * @author Pengtao Qiu
 */
public class LineColor implements Serializable {

    private static final long serialVersionUID = 1L;

    private String r;
    private String g;
    private String b;

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return "LineColor{" +
                "r='" + r + '\'' +
                ", g='" + g + '\'' +
                ", b='" + b + '\'' +
                '}';
    }
}
