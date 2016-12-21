package com.firefly.template2;

import com.firefly.template2.model.VariableStorage;
import com.firefly.template2.model.impl.VariableStorageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                     String fileName = p.getFileName().toString();
                     String className = fileName.substring(0, fileName.length() - ".class".length());
                     try {
                         loadClass(className, Paths.get(fileName));
                     } catch (Exception e) {
                         log.error("load class exception", e);
                     }
                 });
        } catch (IOException e) {
            log.error("read class file exception", e);
        }
    }

    private Class<?> loadClass(String name, Path path) throws Exception {
        classFilePath.putIfAbsent(name, path);
        Class<?> clazz = classLoader.loadClass(name);
        if (clazz != null) {
            map.put(name, (TemplateRenderer) clazz.newInstance());
        }
        return clazz;
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
