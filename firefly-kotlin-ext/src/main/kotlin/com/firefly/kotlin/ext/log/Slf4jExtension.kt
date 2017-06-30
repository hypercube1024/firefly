package com.firefly.kotlin.ext.log

import com.firefly.kotlin.ext.common.KotlinNameResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Pengtao Qiu
 */
object Log {
    fun getLogger(func: () -> Unit): Logger = LoggerFactory.getLogger(KotlinNameResolver.name(func))

    fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)

    fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
}

/**
 * Lazy add a log message if isTraceEnabled is true
 */
fun Logger.trace(msg: () -> Any?) {
    if (isTraceEnabled) trace(msg.toStringSafe())
}

/**
 * Lazy add a log message if isDebugEnabled is true
 */
fun Logger.debug(msg: () -> Any?) {
    if (isDebugEnabled) debug(msg.toStringSafe())
}

/**
 * Lazy add a log message if isInfoEnabled is true
 */
fun Logger.info(msg: () -> Any?) {
    if (isInfoEnabled) info(msg.toStringSafe())
}

/**
 * Lazy add a log message if isWarnEnabled is true
 */
fun Logger.warn(msg: () -> Any?) {
    if (isWarnEnabled) warn(msg.toStringSafe())
}

/**
 * Lazy add a log message if isErrorEnabled is true
 */
fun Logger.error(msg: () -> Any?) {
    if (isErrorEnabled) error(msg.toStringSafe())
}

/**
 * Lazy add a log message with throwable payload if isTraceEnabled is true
 */
fun Logger.trace(t: Throwable, msg: () -> Any?) {
    if (isTraceEnabled) trace(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isDebugEnabled is true
 */
fun Logger.debug(t: Throwable, msg: () -> Any?) {
    if (isDebugEnabled) debug(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isInfoEnabled is true
 */
fun Logger.info(t: Throwable, msg: () -> Any?) {
    if (isInfoEnabled) info(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isWarnEnabled is true
 */
fun Logger.warn(t: Throwable, msg: () -> Any?) {
    if (isWarnEnabled) warn(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isErrorEnabled is true
 */
fun Logger.error(t: Throwable, msg: () -> Any?) {
    if (isErrorEnabled) error(msg.toStringSafe(), t)
}

private fun (() -> Any?).toStringSafe(): String {
    try {
        return invoke().toString()
    } catch (e: Exception) {
        return "Log message invocation exception: $e"
    }
}