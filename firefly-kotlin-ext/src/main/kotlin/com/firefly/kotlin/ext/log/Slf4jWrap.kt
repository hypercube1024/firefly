package com.firefly.kotlin.ext.log

import com.firefly.kotlin.ext.common.KotlinNameResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Pengtao Qiu
 */
object Log {
    fun getLogger(func: () -> Unit): Logger {
        return LoggerFactory.getLogger(KotlinNameResolver.name(func))
    }
}