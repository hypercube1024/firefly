package com.firefly.mvc.web.support;

import com.firefly.annotation.Component;
import com.firefly.annotation.Controller;
import com.firefly.annotation.Interceptor;
import com.firefly.annotation.RequestMapping;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanReader;
import com.firefly.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WebBeanReader extends AnnotationBeanReader {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public WebBeanReader() {
        this(null);
    }

    public WebBeanReader(String file) {
        super(file);
    }

    @Override
    protected BeanDefinition getBeanDefinition(Class<?> c) {
        if (c.isAnnotationPresent(Controller.class) || c.isAnnotationPresent(Component.class)) {
            log.info("classes [{}]", c.getName());
            return componentParser(c);
        } else if (c.isAnnotationPresent(Interceptor.class)) {
            log.info("classes [{}]", c.getName());
            return interceptorParser(c);
        } else
            return null;
    }

    @Override
    protected BeanDefinition componentParser(Class<?> c) {
        ControllerBeanDefinition beanDefinition = new ControllerAnnotatedBeanDefinition();
        setWebBeanDefinition(beanDefinition, c);

        List<Method> reqMethods = getReqMethods(c);
        beanDefinition.setReqMethods(reqMethods);
        return beanDefinition;
    }

    private BeanDefinition interceptorParser(Class<?> c) {
        InterceptorBeanDefinition beanDefinition = new InterceptorAnnotatedBeanDefinition();
        setWebBeanDefinition(beanDefinition, c);

        beanDefinition.setDisposeMethod(getInterceptors(c));

        String uriPattern = c.getAnnotation(Interceptor.class).uri();
        beanDefinition.setUriPattern(uriPattern);

        Integer order = c.getAnnotation(Interceptor.class).order();
        beanDefinition.setOrder(order);
        return beanDefinition;
    }

    private void setWebBeanDefinition(AnnotationBeanDefinition beanDefinition, Class<?> c) {
        beanDefinition.setClassName(c.getName());
        beanDefinition.setId(getId(c));
        beanDefinition.setInterfaceNames(ReflectUtils.getInterfaceNames(c));
        beanDefinition.setInjectFields(getInjectField(c));
        beanDefinition.setInjectMethods(getInjectMethod(c));
        beanDefinition.setConstructor(getInjectConstructor(c));
        beanDefinition.setInitMethod(getInitMethod(c));
    }

    private String getId(Class<?> c) {
        if (c.isAnnotationPresent(Controller.class))
            return c.getAnnotation(Controller.class).value();
        else if (c.isAnnotationPresent(Interceptor.class))
            return c.getAnnotation(Interceptor.class).value();
        else if (c.isAnnotationPresent(Component.class))
            return c.getAnnotation(Component.class).value();
        else
            return "";
    }

    private List<Method> getReqMethods(Class<?> c) {
        Method[] methods = c.getMethods();
        List<Method> list = new ArrayList<Method>();
        for (Method m : methods) {
            if (m.isAnnotationPresent(RequestMapping.class)) {
                list.add(m);
            }
        }
        return list;
    }

    private Method getInterceptors(Class<?> c) {
        for (Method m : c.getMethods()) {// appoints method name is "dispose"
            if (m.getName().equals("dispose"))
                return m;
        }
        return null;
    }
}
