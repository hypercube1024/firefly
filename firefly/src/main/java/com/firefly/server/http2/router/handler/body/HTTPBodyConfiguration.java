package com.firefly.server.http2.router.handler.body;

import javax.servlet.MultipartConfigElement;

/**
 * @author Pengtao Qiu
 */
public class HTTPBodyConfiguration {

    private int bodyBufferThreshold = 512 * 1024;
    private int maxRequestSize = 64 * 1024 * 1024;
    private int maxFileSize = 64 * 1024 * 1024;
    private String tempFilePath = System.getProperty("java.io.tmpdir");
    private String charset = "UTF-8";
    private MultipartConfigElement multipartConfigElement = new MultipartConfigElement(tempFilePath, maxFileSize, maxRequestSize, bodyBufferThreshold);

    public int getBodyBufferThreshold() {
        return bodyBufferThreshold;
    }

    public void setBodyBufferThreshold(int bodyBufferThreshold) {
        this.bodyBufferThreshold = bodyBufferThreshold;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public MultipartConfigElement getMultipartConfigElement() {
        return multipartConfigElement;
    }

    public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }
}
