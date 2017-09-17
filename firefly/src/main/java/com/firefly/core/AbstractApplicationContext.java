package com.firefly.core;

import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.exception.BeanDefinitionParsingException;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

abstract public class AbstractApplicationContext implements ApplicationContext {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    protected Map<String, Object> map = new HashMap<>();
    protected Set<String> errorMemo = new HashSet<>();
    protected List<BeanDefinition> beanDefinitions;
    protected List<Pair<Method, Object>> destroyedMethods = new ArrayList<>();
    protected List<Pair<Method, Object>> initMethods = new ArrayList<>();

    public AbstractApplicationContext() {
        this(null);
    }

    public AbstractApplicationContext(String file) {
        beanDefinitions = getBeanDefinitions(file);
        beanDefinitionCheck(); // Conflicts check
        addObjectToContext();
        if (!initMethods.isEmpty()) {
            invokeMethods(initMethods);
        }
        if (!destroyedMethods.isEmpty()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> invokeMethods(destroyedMethods), "the firefly shutdown thread"));
        }
        map = Collections.unmodifiableMap(map);
    }

    protected void invokeMethods(List<Pair<Method, Object>> methods) {
        methods.forEach(pair -> {
            try {
                pair.first.invoke(pair.second);
            } catch (Exception e) {
                log.error("invoke method exception", e);
            }
        });
    }

    private void addObjectToContext() {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            inject(beanDefinition);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(Class<T> clazz) {
        return (T) map.get(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(String id) {
        return (T) map.get(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> getBeans(Class<T> clazz) {
        Set<?> list = map.values().stream()
                         .filter(v -> clazz.isAssignableFrom(v.getClass()))
                         .collect(Collectors.toSet());
        return (Set<T>) list;
    }

    @Override
    public Map<String, Object> getBeanMap() {
        return map;
    }

    /**
     * Bean definition conflict check
     */
    protected void beanDefinitionCheck() {
        for (int i = 0; i < beanDefinitions.size(); i++) {
            for (int j = i + 1; j < beanDefinitions.size(); j++) {
                BeanDefinition b1 = beanDefinitions.get(i);
                BeanDefinition b2 = beanDefinitions.get(j);

                // Two components' id are equal
                if (VerifyUtils.isNotEmpty(b1.getId())
                        && VerifyUtils.isNotEmpty(b2.getId())
                        && b1.getId().equals(b2.getId())) {
                    error("Duplicated bean ID of " + b1.getClassName() + " and " + b2.getClassName());
                }

                if (b1.getClassName().equals(b2.getClassName())) {
                    // Two components' class name are equal, but one of them does not set id.
                    if (VerifyUtils.isEmpty(b1.getId()) || VerifyUtils.isEmpty(b2.getId())) {
                        error("Duplicated class definition. Please set a ID for " + b1.getClassName());
                    } else {
                        // Their id are different, save them to memo.
                        // When the component is injecting by type, throw an exception.
                        errorMemo.add(b1.getClassName());
                    }
                }

                for (String iname1 : b1.getInterfaceNames()) {
                    for (String iname2 : b2.getInterfaceNames()) {
                        if (iname1.equals(iname2)) {
                            // Two components' interface name are equal, but one of them does not set id.
                            if (VerifyUtils.isEmpty(b1.getId()) || VerifyUtils.isEmpty(b2.getId())) {
                                error("Duplicated class definition. Please set a ID for " + b1.getClassName());
                            } else {
                                // Their id are different, save them to memo.
                                // When the component is injecting by type, throw an exception.
                                errorMemo.add(iname1);
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * Type injecting check.
     *
     * @param key bean name
     */
    protected void check(String key) {
        if (errorMemo.contains(key)) {
            error(key + " auto inject failure! More the one bean are found");
        }
    }

    protected void addObjectToContext(final BeanDefinition beanDefinition, final Object object) {
        // context key using id
        String id = beanDefinition.getId();
        if (VerifyUtils.isNotEmpty(id)) {
            map.put(id, object);
        }

        // context key using class name
        map.put(beanDefinition.getClassName(), object);

        // context key using interface name
        String[] keys = beanDefinition.getInterfaceNames();
        for (String k : keys) {
            map.put(k, object);
        }

        if (log.isDebugEnabled()) {
            log.debug("add object [{}] - [{}] to context", beanDefinition.getClass(), object.toString());
        }

        // invoke initial method
        Method initMethod = beanDefinition.getInitMethod();
        if (initMethod != null) {
            Pair<Method, Object> pair = new Pair<>(initMethod, object);
            initMethods.add(pair);
        }

        Method destroyedMethod = beanDefinition.getDestroyedMethod();
        if (destroyedMethod != null) {
            Pair<Method, Object> pair = new Pair<>(destroyedMethod, object);
            destroyedMethods.add(pair);
        }
    }

    protected BeanDefinition findBeanDefinition(String key) {
        check(key);
        for (BeanDefinition beanDefinition : beanDefinitions) {
            if (key.equals(beanDefinition.getId())) {
                return beanDefinition;
            } else if (key.equals(beanDefinition.getClassName())) {
                return beanDefinition;
            } else {
                for (String interfaceName : beanDefinition.getInterfaceNames()) {
                    if (key.equals(interfaceName)) {
                        return beanDefinition;
                    }
                }
            }
        }
        return null;
    }

    protected void error(String msg) {
        log.error(msg);
        throw new BeanDefinitionParsingException(msg);
    }

    abstract protected List<BeanDefinition> getBeanDefinitions(String file);

    abstract protected Object inject(BeanDefinition beanDef);

}
