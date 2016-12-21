package com.firefly.template2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class Template2Factory {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final Configuration configuration;
    private final Template2Compiler compiler;
    private final Template2Loader loader;

    public Template2Factory(Configuration configuration) {
        this.configuration = configuration;
        compiler = new Template2Compiler(configuration);
        loader = new Template2Loader();
        Map<String, File> javaFiles = compiler.generateJavaFiles();
        compiler.compileJavaFiles(javaFiles);
        loader.loadAllClass(configuration.getRootPath().toPath());
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Template2Compiler getCompiler() {
        return compiler;
    }

    public Template2Loader getLoader() {
        return loader;
    }

    public void render(String name, OutputStream outputStream, Map<String, Object> parameters) {
        this.render(name, outputStream, parameters, null);
    }

    public void render(String name, OutputStream outputStream, Map<String, Object> parameters, Map<String, Object> globalVar) {
        String className = configuration.getPackagePrefix() + "." + name;
        loader.render(className, outputStream, parameters, globalVar);
    }

    public String render(String name, Map<String, Object> parameters, Map<String, Object> globalVar) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            this.render(name, out, parameters, globalVar);
            return out.toString(configuration.getOutputJavaFileCharset());
        } catch (IOException e) {
            log.error("render template exception", e);
            return null;
        }
    }

    public String render(String name, Map<String, Object> parameters) {
        return this.render(name, parameters, null);
    }

}
