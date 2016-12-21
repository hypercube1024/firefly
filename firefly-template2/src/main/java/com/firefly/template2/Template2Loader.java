package com.firefly.template2;

import com.firefly.template2.model.VariableStorage;
import com.firefly.template2.model.impl.VariableStorageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pengtao Qiu
 */
public class Template2Loader {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final Map<String, TemplateRenderer> map = new ConcurrentHashMap<>();
    private final Map<String, Path> classFilePath = new ConcurrentHashMap<>();
    private final Template2ClassLoader classLoader = new Template2ClassLoader(Template2Loader.class.getClassLoader());

    public void loadAllClass(Path root) {
        try {
            Files.walk(root)
                 .filter(p -> p.getFileName().toString().endsWith(".class"))
                 .forEach(p -> {
                     String fileName = p.toFile().getAbsolutePath();
                     String rootFilePath = root.toFile().getAbsolutePath();
                     String className = fileName.substring(rootFilePath.length() + 1, fileName.length() - ".class".length()).replace(File.separatorChar, '.');
                     loadClass(className, p);
                 });
        } catch (IOException e) {
            log.error("read class file exception", e);
        }
    }

    public void loadClass(String className, Path path) {
        try {
            classFilePath.putIfAbsent(className, path);
            Class<?> clazz = classLoader.loadClass(className);
            if (clazz != null) {
                map.put(className, (TemplateRenderer) clazz.newInstance());
            }
        } catch (Exception e) {
            log.error("load class exception", e);
        }
    }

    public void render(String name, OutputStream outputStream, Map<String, Object> parameters) {
        this.render(name, outputStream, parameters, null);
    }

    public void render(String name, OutputStream outputStream, Map<String, Object> parameters, Map<String, Object> globalVar) {
        TemplateRenderer renderer = map.get(name);
        if (renderer != null) {
            VariableStorage var = globalVar != null ? new VariableStorageImpl(Collections.singleton(globalVar)) : new VariableStorageImpl();
            var.callAction(() -> {
                try {
                    renderer.main(outputStream, var);
                } catch (IOException e) {
                    log.error("template rendering exception", e);
                }
            }, parameters);
        }
    }

    private class Template2ClassLoader extends ClassLoader {

        private Template2ClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) {
            try {
                byte[] data = Files.readAllBytes(classFilePath.get(name));
                return defineClass(name, data, 0, data.length);
            } catch (IOException e) {
                log.error("read class file exception", e);
                throw new RuntimeException(e);
            }
        }
    }
}
