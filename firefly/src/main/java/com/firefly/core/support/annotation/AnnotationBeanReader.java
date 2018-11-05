package com.firefly.core.support.annotation;

import com.firefly.annotation.Component;
import com.firefly.core.support.AbstractBeanReader;
import com.firefly.core.support.BeanDefinition;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import static com.firefly.core.support.annotation.AnnotationBeanUtils.*;

/**
 * Annotation Bean processor
 *
 * @author AlvinQiu
 */
public class AnnotationBeanReader extends AbstractBeanReader {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private String fileSeparator = System.getProperty("file.separator");

    public AnnotationBeanReader() {
        this(null);
    }

    public AnnotationBeanReader(String file) {
        beanDefinitions = new ArrayList<>();
        Config config = ConfigReader.getInstance().load(file);
        for (String pack : config.getPaths()) {
            log.info("componentPath [{}]", pack);
            scan(pack.trim());
        }
    }

    private void scan(String packageName) {
        String packageDirName = packageName.replace('.', '/');
        log.debug("package directory name -> {} ", packageDirName);
        URL url = AnnotationBeanReader.class.getClassLoader().getResource(packageDirName);
        if (url == null) {
            error(packageName + " can not be found");
        } else {
            log.debug("URL -> {}", url.toString());
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                parseFile(url, packageDirName);
            } else if ("jar".equals(protocol)) {
                parseJar(url, packageDirName);
            }
        }
    }

    private void parseFile(URL url, final String packageDirName) {
        try {
            FileUtils.filter(Paths.get(url.toURI()), "*.class", p -> {
                String pathName = p.toString();
                if (!pathName.contains("$")) {
                    String name = pathName.replace(fileSeparator, ".");
                    String packageName = packageDirName.replace('/', '.');
                    name = name.substring(name.indexOf(packageName), name.length() - 6);
                    parseClass(name);
                }
            });
        } catch (Throwable t) {
            log.error("parse file error", t);
        }
    }

    private void parseJar(URL url, String packageDirName) {
        try {
            Enumeration<JarEntry> entries = ((JarURLConnection) url.openConnection()).getJarFile().entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.contains("$") || !name.startsWith(packageDirName + "/")) {
                    continue;
                }
                parseClass(name.substring(0, name.length() - 6).replace('/', '.'));
            }
        } catch (IOException t) {
            log.error("parse jar error", t);
        }
    }

    private void parseClass(String className) {
        try {
            Class<?> c = AnnotationBeanReader.class.getClassLoader().loadClass(className);
            BeanDefinition beanDefinition = getBeanDefinition(c);
            if (beanDefinition != null) {
                beanDefinitions.add(beanDefinition);
            }
        } catch (Throwable t) {
            log.error("parse class error", t);
        }
    }

    protected BeanDefinition getBeanDefinition(Class<?> c) {
        if (c.isAnnotationPresent(Component.class)) {
            log.info("classes [{}]", c.getName());
            return componentParser(c);
        } else {
            return null;
        }
    }

    protected BeanDefinition componentParser(Class<?> c) {
        AnnotationBeanDefinition annotationBeanDefinition = new AnnotatedBeanDefinition();
        annotationBeanDefinition.setClassName(c.getName());
        annotationBeanDefinition.setId(c.getAnnotation(Component.class).value());
        annotationBeanDefinition.setInterfaceNames(ReflectUtils.getInterfaceNames(c));
        annotationBeanDefinition.setInjectFields(getInjectFields(c));
        annotationBeanDefinition.setInjectMethods(getInjectMethods(c));
        annotationBeanDefinition.setConstructor(getInjectConstructor(c));
        annotationBeanDefinition.setInitMethod(getInitMethod(c));
        annotationBeanDefinition.setDestroyedMethod(getDestroyedMethod(c));
        return annotationBeanDefinition;
    }

}
