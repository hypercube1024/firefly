package com.firefly.server.http2.router.handler.file;

/**
 * @author Pengtao Qiu
 */
public class StaticFileConfiguration {

    private String rootPath;
    private int maxRangePart = 8;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public int getMaxRangePart() {
        return maxRangePart;
    }

    public void setMaxRangePart(int maxRangePart) {
        this.maxRangePart = maxRangePart;
    }
}
