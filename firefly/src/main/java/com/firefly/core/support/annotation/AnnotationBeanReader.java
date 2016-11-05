package com.firefly.core.support.annotation;

import com.firefly.annotation.Component;
import com.firefly.annotation.InitialMethod;
import com.firefly.annotation.Inject;
import com.firefly.core.support.AbstractBeanReader;
import com.firefly.core.support.BeanDefinition;
import com.firefly.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * Annotation Bean processor
 *
 * @author AlvinQiu
 */
public class AnnotationBeanReader extends AbstractBeanReader {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public AnnotationBeanReader() {
        this(null);
    }

    public AnnotationBeanReader(String file) {
        beanDefinitions = new ArrayList<BeanDefinition>();
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
        }
        log.debug("URL -> {}", url.toString());
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            parseFile(url, packageDirName);
        } else if ("jar".equals(protocol)) {
            parseJar(url, packageDirName);
        }
    }

    private void parseFile(URL url, final String packageDirName) {
        File path = null;
        try {
            path = new File(url.toURI());
        } catch (Throwable t) {
            log.error("parse file error", t);
        }
        path.listFiles((File file) -> {
            String name = file.getName();
            if (name.endsWith(".class") && !name.contains("$")) {
                parseClass(packageDirName.replace('/', '.') + "."
                        + name.substring(0, file.getName().length() - 6));
            } else if (file.isDirectory()) {
                try {
                    parseFile(file.toURI().toURL(), packageDirName + "/"
                            + name);
                } catch (Throwable t) {
                    log.error("parse file error", t);
                }
            }
            return false;
        });
    }

    private void parseJar(URL url, String packageDirName) {
        Enumeration<JarEntry> entries = null;
        try {
            entries = ((JarURLConnection) url.openConnection()).getJarFile()
                    .entries();
        } catch (Throwable t) {
            log.error("parse jar error", t);
        }
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (!name.endsWith(".class") || name.contains("$")
                    || !name.startsWith(packageDirName + "/"))
                continue;
            parseClass(name.substring(0, name.length() - 6).replace('/', '.'));
        }

    }

    private void parseClass(String className) {
        Class<?> c = null;
        try {
            c = AnnotationBeanReader.class.getClassLoader()
                    .loadClass(className);
        } catch (Throwable t) {
            log.error("parse class error", t);
        }

        BeanDefinition beanDefinition = getBeanDefinition(c);
        if (beanDefinition != null)
            beanDefinitions.add(beanDefinition);
    }

    protected BeanDefinition getBeanDefinition(Class<?> c) {
        if (c.isAnnotationPresent(Component.class)) {
            log.info("classes [{}]", c.getName());
            return componentParser(c);
        } else
            return null;
    }

    protected BeanDefinition componentParser(Class<?> c) {
        AnnotationBeanDefinition annotationBeanDefinition = new AnnotatedBeanDefinition();
        annotationBeanDefinition.setClassName(c.getName());
        annotationBeanDefinition.setId(c.getAnnotation(Component.class).value());
        annotationBeanDefinition.setInterfaceNames(ReflectUtils.getInterfaceNames(c));
        annotationBeanDefinition.setInjectFields(getInjectField(c));
        annotationBeanDefinition.setInjectMethods(getInjectMethod(c));
        annotationBeanDefinition.setConstructor(getInjectConstructor(c));
        annotationBeanDefinition.setInitMethod(getInitMethod(c));
        return annotationBeanDefinition;
    }

    protected Method getInitMethod(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(InitialMethod.class)) {
                return m;
            }
        }
        return null;
    }

    protected Constructor<?> getInjectConstructor(Class<?> c) {
        for (Constructor<?> constructor : c.getConstructors()) {
            if (constructor.getAnnotation(Inject.class) != null) {
                return constructor;
            }
        }
        try {
            return c.getConstructor(new Class<?>[0]);
        } catch (Throwable t) {
            log.error("gets non-parameter constructor error", t);
            return null;
        }
    }

    protected List<Field> getInjectField(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        List<Field> list = new ArrayList<>();
        for (Field field : fields) {
            if (field.getAnnotation(Inject.class) != null) {
                list.add(field);
            }
        }
        return list;
    }

    protected List<Method> getInjectMethod(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        List<Method> list = new ArrayList<>();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Inject.class)) {
                list.add(m);
            }
        }
        return list;
    }

}
