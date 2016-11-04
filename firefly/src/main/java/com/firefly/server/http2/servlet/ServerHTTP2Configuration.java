package com.firefly.server.http2.servlet;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.mvc.web.FileAccessFilter;
import com.firefly.server.http2.servlet.session.HttpSessionManager;
import com.firefly.server.http2.servlet.session.LocalHttpSessionManager;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Map;

public class ServerHTTP2Configuration extends HTTP2Configuration {

    public static final String DEFAULT_CONFIG_FILE_NAME = "firefly.xml";

    private String host;
    private int port;

    // servlet server settings
    private int httpBodyThreshold = 4 * 1024 * 1024;
    private String temporaryDirectory = new File(System.getProperty("user.dir"), "temp").getAbsolutePath();
    private int servletResponseBufferSize = 8 * 1024;
    private MultipartConfigElement multipartConfigElement;
    private String configFileName = DEFAULT_CONFIG_FILE_NAME;
    private String serverHome;
    private int maxRangeNum = 8;
    private Map<Integer, String> errorPage;
    private FileAccessFilter fileAccessFilter = new FileAccessFilter() {
        @Override
        public String doFilter(HttpServletRequest request, HttpServletResponse response, String path) {
            return path;
        }
    };

    // servlet session settings
    private String sessionIdName = "jsessionid";
    private HttpSessionManager httpSessionManager = new LocalHttpSessionManager();

    // asynchronous context pool settings
    private int asynchronousContextCorePoolSize = Runtime.getRuntime().availableProcessors();
    private int asynchronousContextMaximumPoolSize = 64;
    private int asynchronousContextCorePoolKeepAliveTime = 10 * 1000;
    private int asynchronousContextTimeout = 6 * 1000;

    public int getHttpBodyThreshold() {
        return httpBodyThreshold;
    }

    public void setHttpBodyThreshold(int httpBodyThreshold) {
        this.httpBodyThreshold = httpBodyThreshold;
    }

    public String getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public void setTemporaryDirectory(String temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory;
    }

    public int getServletResponseBufferSize() {
        return servletResponseBufferSize;
    }

    public void setServletResponseBufferSize(int servletResponseBufferSize) {
        this.servletResponseBufferSize = servletResponseBufferSize;
    }

    public MultipartConfigElement getMultipartConfigElement() {
        return multipartConfigElement;
    }

    public MultipartConfigElement getDefaultMultipartConfigElement() {
        if (multipartConfigElement != null) {
            return multipartConfigElement;
        } else {
            multipartConfigElement = new MultipartConfigElement(temporaryDirectory, -1, -1, httpBodyThreshold);
            return multipartConfigElement;
        }
    }

    public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public String getServerHome() {
        return serverHome;
    }

    public void setServerHome(String serverHome) {
        this.serverHome = serverHome;
    }

    public int getMaxRangeNum() {
        return maxRangeNum;
    }

    public void setMaxRangeNum(int maxRangeNum) {
        this.maxRangeNum = maxRangeNum;
    }

    public Map<Integer, String> getErrorPage() {
        return errorPage;
    }

    public void setErrorPage(Map<Integer, String> errorPage) {
        this.errorPage = errorPage;
    }

    public FileAccessFilter getFileAccessFilter() {
        return fileAccessFilter;
    }

    public void setFileAccessFilter(FileAccessFilter fileAccessFilter) {
        this.fileAccessFilter = fileAccessFilter;
    }

    public String getSessionIdName() {
        return sessionIdName;
    }

    public void setSessionIdName(String sessionIdName) {
        this.sessionIdName = sessionIdName;
    }

    public HttpSessionManager getHttpSessionManager() {
        return httpSessionManager;
    }

    public void setHttpSessionManager(HttpSessionManager httpSessionManager) {
        this.httpSessionManager = httpSessionManager;
    }

    public int getAsynchronousContextCorePoolSize() {
        return asynchronousContextCorePoolSize;
    }

    public void setAsynchronousContextCorePoolSize(int asynchronousContextCorePoolSize) {
        this.asynchronousContextCorePoolSize = asynchronousContextCorePoolSize;
    }

    public int getAsynchronousContextMaximumPoolSize() {
        return asynchronousContextMaximumPoolSize;
    }

    public void setAsynchronousContextMaximumPoolSize(int asynchronousContextMaximumPoolSize) {
        this.asynchronousContextMaximumPoolSize = asynchronousContextMaximumPoolSize;
    }

    public int getAsynchronousContextCorePoolKeepAliveTime() {
        return asynchronousContextCorePoolKeepAliveTime;
    }

    public void setAsynchronousContextCorePoolKeepAliveTime(int asynchronousContextCorePoolKeepAliveTime) {
        this.asynchronousContextCorePoolKeepAliveTime = asynchronousContextCorePoolKeepAliveTime;
    }

    public int getAsynchronousContextTimeout() {
        return asynchronousContextTimeout;
    }

    public void setAsynchronousContextTimeout(int asynchronousContextTimeout) {
        this.asynchronousContextTimeout = asynchronousContextTimeout;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
