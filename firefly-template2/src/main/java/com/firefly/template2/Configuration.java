package com.firefly.template2;

/**
 * @author Pengtao Qiu
 */
public class Configuration {

    private String templateHome;
    private String suffix = "fftl";
    private String charset = "UTF-8";

    public String getTemplateHome() {
        return templateHome;
    }

    public void setTemplateHome(String templateHome) {
        this.templateHome = templateHome;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
