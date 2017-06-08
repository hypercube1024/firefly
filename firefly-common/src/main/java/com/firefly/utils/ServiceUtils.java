package com.firefly.utils;

import java.util.ServiceLoader;

/**
 * @author Pengtao Qiu
 */
abstract public class ServiceUtils {

    public static <T> T loadService(Class<T> clazz, T defaultService) {
        T service = null;
        ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz);
        for (T t : serviceLoader) {
            service = t;
        }
        if (service == null) {
            service = defaultService;
        }
        return service;
    }
}
