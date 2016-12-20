package com.firefly.template2;

import com.firefly.template2.utils.PathUtils;

import java.io.File;

/**
 * @author Pengtao Qiu
 */
public class Configuration {

    private String templateHome;
    private String javaFileOutputPath = "_compiled_java_file";
    private String suffix = "fftl";
    private String templateCharset = "UTF-8";
    private String outputJavaFileCharset = "UTF-8";
    private String packagePrefix = "com.firefly.template2.compiled";
    private String lineSeparator = System.getProperty("line.separator");

    public String getTemplateHome() {
        return templateHome;
    }

    public void setTemplateHome(String templateHome) {
        this.templateHome = PathUtils.removeTheLastPathSeparator(templateHome);
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public String getTemplateCharset() {
        return templateCharset;
    }

    public void setTemplateCharset(String templateCharset) {
        this.templateCharset = templateCharset;
    }

    public String getOutputJavaFileCharset() {
        return outputJavaFileCharset;
    }

    public void setOutputJavaFileCharset(String outputJavaFileCharset) {
        this.outputJavaFileCharset = outputJavaFileCharset;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getJavaFileOutputPath() {
        return javaFileOutputPath;
    }

    public void setJavaFileOutputPath(String javaFileOutputPath) {
        this.javaFileOutputPath = PathUtils.removeTheLastPathSeparator(javaFileOutputPath);
    }

    public File getRootPath() {
        return new File(getTemplateHome(), getJavaFileOutputPath());
    }
}
