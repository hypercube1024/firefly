package com.firefly.core;

import java.util.Collection;
import java.util.Map;

public interface ApplicationContext {

    /**
     * Get a bean by type
     *
     * @param clazz Bean's class object
     * @param <T>   Bean's type
     * @return The managed bean
     */
    <T> T getBean(Class<T> clazz);

    /**
     * Get a bean by id
     *
     * @param id  Bean's id
     * @param <T> Bean type
     * @return The managed bean
     */
    <T> T getBean(String id);

    /**
     * Get all beans by type
     *
     * @param clazz Bean's class object
     * @param <T>   Bean type
     * @return All beans are derived from type
     */
    <T> Collection<T> getBeans(Class<T> clazz);

    /**
     * Get all managed beans
     *
     * @return The unmodifiable map of all beans
     */
    Map<String, Object> getBeanMap();
}
