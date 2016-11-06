package com.firefly.core;

import com.firefly.annotation.Inject;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanDefinition;
import com.firefly.core.support.annotation.AnnotationBeanReader;
import com.firefly.core.support.xml.*;
import com.firefly.utils.ConvertUtils;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.BeanMethodFilter;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The core application context mixed XML and annotation bean management
 *
 * @author JJ Xu &amp; Alvin Qiu
 */
public class XmlApplicationContext extends AbstractApplicationContext {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    public XmlApplicationContext() {
        this(null);
    }

    public XmlApplicationContext(String file) {
        super(file);
    }

    @Override
    protected List<BeanDefinition> getBeanDefinitions(String file) {
        List<BeanDefinition> list1 = new AnnotationBeanReader(file)
                .loadBeanDefinitions();
        List<BeanDefinition> list2 = new XmlBeanReader(file)
                .loadBeanDefinitions();
        if (list1 != null && list2 != null) {
            log.debug("mixed bean");
            list1.addAll(list2);
            return list1;
        } else if (list1 != null) {
            log.debug("annotation bean");
            return list1;
        } else if (list2 != null) {
            log.debug("xml bean");
            return list2;
        }
        return null;
    }

    @Override
    protected Object inject(BeanDefinition beanDef) {
        if (beanDef instanceof XmlBeanDefinition)
            return xmlInject(beanDef);
        else if (beanDef instanceof AnnotationBeanDefinition)
            return annotationInject(beanDef);
        else
            return null;
    }

    private Object getInstance(BeanDefinition beanDef) {
        if (StringUtils.hasText(beanDef.getId())) {
            return map.get(beanDef.getId());
        } else {
            Object instance = map.get(beanDef.getClassName());
            if (instance != null) {
                return instance;
            }

            String[] keys = beanDef.getInterfaceNames();
            for (String k : keys) {
                instance = map.get(beanDef);
                if (instance != null) {
                    return instance;
                }
            }
            return instance;
        }
    }

    private Object xmlInject(BeanDefinition beanDef) {
        Object instance = getInstance(beanDef);
        if (instance == null) {
            final XmlBeanDefinition beanDefinition = (XmlBeanDefinition) beanDef;
            Class<?> clazz = null;

            try {
                clazz = XmlApplicationContext.class.getClassLoader().loadClass(beanDefinition.getClassName());
                if (beanDefinition.getContructorParameters().size() <= 0) {
                    instance = clazz.newInstance();
                } else {
                    List<Object> constructorParameters = new ArrayList<>();
                    for (int i = 0; i < beanDefinition.getContructorParameters().size(); i++) {
                        Object p = getInjectArg(beanDefinition.getContructorParameters().get(i), beanDefinition.getConstructor().getParameterTypes()[i]);
                        constructorParameters.add(p);
                    }
                    instance = beanDefinition.getConstructor().newInstance(constructorParameters.toArray());
                }
            } catch (Throwable t) {
                log.error("object initiate error", t);
            }

            if (instance != null) {
                final Object obj = instance;
                ReflectUtils.getSetterMethods(clazz, (String propertyName, Method method) -> {
                    XmlManagedNode value = beanDefinition.getProperties().get(propertyName);
                    if (value != null) {
                        try {
                            method.invoke(obj, getInjectArg(value, method.getParameterTypes()[0]));
                        } catch (Throwable t) {
                            log.error("xml inject error", t);
                        }
                    }
                    return false;
                });
            } else {
                error("initialize XML bean exception, the instance is null");
            }

            addObjectToContext(beanDefinition, instance);
            return instance;
        } else {
            return instance;
        }
    }

    @SuppressWarnings("unchecked")
    private Object getInjectArg(XmlManagedNode value, Class<?> parameterType) {
        if (value instanceof ManagedValue) {
            ManagedValue managedValue = (ManagedValue) value;
            String typeName = null;
            if (parameterType == null) {
                typeName = VerifyUtils.isEmpty(managedValue.getTypeName()) ? null
                        : managedValue.getTypeName();
            } else {
                typeName = VerifyUtils.isEmpty(managedValue.getTypeName()) ? parameterType.getName() : managedValue.getTypeName();
            }
            log.debug("value type [{}]", typeName);
            return getValueArg(managedValue, typeName);
        } else if (value instanceof ManagedRef) {
            return getRefArg((ManagedRef) value);
        } else if (value instanceof ManagedList) {
            return getListArg((ManagedList<XmlManagedNode>) value, parameterType);
        } else if (value instanceof ManagedArray) {
            return getArrayArg((ManagedArray<XmlManagedNode>) value, parameterType);
        } else if (value instanceof ManagedMap) {
            return getMapArg((ManagedMap<XmlManagedNode, XmlManagedNode>) value, parameterType);
        } else
            return null;
    }

    private Object getValueArg(ManagedValue managedValue, String typeName) {
        return ConvertUtils.convert(managedValue.getValue(), typeName);
    }

    private Object getRefArg(ManagedRef ref) {
        Object instance = map.get(ref.getBeanName());
        if (instance == null) {
            BeanDefinition b = findBeanDefinition(ref.getBeanName());
            if (b != null)
                instance = inject(b);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Object getListArg(ManagedList<XmlManagedNode> values, Class<?> setterParamType) {
        Collection<Object> collection = null;

        if (VerifyUtils.isNotEmpty(values.getTypeName())) {
            try {
                collection = (Collection<Object>) XmlApplicationContext.class
                        .getClassLoader().loadClass(values.getTypeName())
                        .newInstance();
            } catch (Throwable t) {
                log.error("list inject error", t);
            }
        } else {
            collection = (setterParamType == null ? new ArrayList<Object>()
                    : ConvertUtils.getCollectionObj(setterParamType));
        }

        for (XmlManagedNode item : values) {
            Object listValue = getInjectArg(item, null);
            collection.add(listValue);
        }
        return collection;
    }

    private Object getArrayArg(ManagedArray<XmlManagedNode> values, Class<?> setterParamType) {
        Collection<Object> collection = new ArrayList<>();
        for (XmlManagedNode item : values) {
            Object listValue = getInjectArg(item, null);
            collection.add(listValue);
        }
        return ConvertUtils.convert(collection, setterParamType);
    }


    @SuppressWarnings("unchecked")
    private Object getMapArg(ManagedMap<XmlManagedNode, XmlManagedNode> values, Class<?> setterParamType) {
        Map<Object, Object> m = null;
        if (VerifyUtils.isNotEmpty(values.getTypeName())) {
            try {
                m = (Map<Object, Object>) XmlApplicationContext.class.getClassLoader()
                        .loadClass(values.getTypeName())
                        .newInstance();
            } catch (Throwable t) {
                log.error("map inject error", t);
            }
        } else {
            m = (setterParamType == null ? new HashMap<>() : ConvertUtils.getMapObj(setterParamType));
            log.debug("map ret [{}]", m.getClass().getName());
        }
        for (XmlManagedNode o : values.keySet()) {
            Object k = getInjectArg(o, null);
            Object v = getInjectArg(values.get(o), null);
            m.put(k, v);
        }
        return m;
    }

    /**
     * annotation injecting
     *
     * @param beanDef
     * @return
     */
    private Object annotationInject(BeanDefinition beanDef) {
        Object instance = getInstance(beanDef);
        if (instance == null) {
            AnnotationBeanDefinition beanDefinition = (AnnotationBeanDefinition) beanDef;
            // constructor injecting
            instance = constructorInject(beanDefinition);
            beanDefinition.setInjectedInstance(instance);
            fieldInject(beanDefinition, instance);
            methodInject(beanDefinition, instance);
            addObjectToContext(beanDefinition, instance);
            return instance;
        } else {
            return instance;
        }
    }

    private Object constructorInject(AnnotationBeanDefinition beanDefinition) {
        Class<?>[] params = beanDefinition.getConstructor().getParameterTypes();
        Object[] p = new Object[params.length];
        injectObject(params, p);
        Object instance = null;
        try {
            instance = beanDefinition.getConstructor().newInstance(p);
        } catch (Throwable t) {
            log.error("constructor injecting error", t);
        }
        return instance;
    }

    private void injectObject(Class<?>[] params, Object[] p) {
        for (int i = 0; i < p.length; i++) {
            String key = params[i].getName();
            Object instance = map.get(key);
            if (instance != null) {
                p[i] = instance;
            } else {
                BeanDefinition b = findBeanDefinition(key);
                if (b != null)
                    p[i] = inject(b);
            }
        }
    }

    private void fieldInject(AnnotationBeanDefinition beanDefinition, final Object object) {
        for (Field field : beanDefinition.getInjectFields()) {
            field.setAccessible(true);
            Class<?> clazz = field.getType();
            String id = field.getAnnotation(Inject.class).value();
            String key = VerifyUtils.isNotEmpty(id) ? id : clazz.getName();
            Object instance = map.get(key);
            if (instance == null) {
                BeanDefinition b = findBeanDefinition(key);
                if (b != null)
                    instance = inject(b);
            }
            if (instance != null) {
                try {
                    field.set(object, instance);
                } catch (Throwable t) {
                    log.error("field injecting error", t);
                }
            }
        }
    }

    private void methodInject(AnnotationBeanDefinition beanDefinition, final Object object) {
        for (Method method : beanDefinition.getInjectMethods()) {
            method.setAccessible(true);
            Class<?>[] params = method.getParameterTypes();
            Object[] p = new Object[params.length];
            injectObject(params, p);
            try {
                method.invoke(object, p);
            } catch (Throwable t) {
                log.error("method injecting error", t);
            }
        }
    }
}
