package com.firefly.example.kotlin.coffee.store.utils

import com.firefly.annotation.Component
import com.firefly.utils.io.ClassRelativeResourceLoader
import com.firefly.utils.io.Resource

/**
 * @author Pengtao Qiu
 */
@Component
class ResourceUtils {

    private val resourceLoader = ClassRelativeResourceLoader(javaClass)

    fun resource(path: String): Resource {
        return resourceLoader.getResource(path)
    }
}