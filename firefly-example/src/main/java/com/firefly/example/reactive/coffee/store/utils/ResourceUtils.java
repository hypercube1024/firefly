package com.firefly.example.reactive.coffee.store.utils;

import com.firefly.annotation.Component;
import com.firefly.utils.io.ClassRelativeResourceLoader;
import com.firefly.utils.io.Resource;

/**
 * @author Pengtao Qiu
 */
@Component
public class ResourceUtils {

    private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());

    public Resource resource(String path) {
        return resourceLoader.getResource(path);
    }
}
