package com.fireflysource.common.slf4j

import com.fireflysource.common.reflect.KotlinNameResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Pengtao Qiu
 */
object KtLogger {
    fun getLogger(func: () -> Unit): Logger = LoggerFactory.getLogger(KotlinNameResolver.name(func))

    fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)

    fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
}

/**
 * Lazy add a log message if isTraceEnabled is true
 */
inline fun Logger.trace(msg: () -> Any?) {
    if (isTraceEnabled) trace(msg.toStringSafe())
}

/**
 * Lazy add a log message if isDebugEnabled is true
 */
inline fun Logger.debug(msg: () -> Any?) {
    if (isDebugEnabled) debug(msg.toStringSafe())
}

/**
 * Lazy add a log message if isInfoEnabled is true
 */
inline fun Logger.info(msg: () -> Any?) {
    if (isInfoEnabled) info(msg.toStringSafe())
}

/**
 * Lazy add a log message if isWarnEnabled is true
 */
inline fun Logger.warn(msg: () -> Any?) {
    if (isWarnEnabled) warn(msg.toStringSafe())
}

/**
 * Lazy add a log message if isErrorEnabled is true
 */
inline fun Logger.error(msg: () -> Any?) {
    if (isErrorEnabled) error(msg.toStringSafe())
}

/**
 * Lazy add a log message with throwable payload if isTraceEnabled is true
 */
inline fun Logger.trace(msg: () -> Any?, t: Throwable) {
    if (isTraceEnabled) trace(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isDebugEnabled is true
 */
inline fun Logger.debug(msg: () -> Any?, t: Throwable) {
    if (isDebugEnabled) debug(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isInfoEnabled is true
 */
inline fun Logger.info(msg: () -> Any?, t: Throwable) {
    if (isInfoEnabled) info(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isWarnEnabled is true
 */
inline fun Logger.warn(msg: () -> Any?, t: Throwable) {
    if (isWarnEnabled) warn(msg.toStringSafe(), t)
}

/**
 * Lazy add a log message with throwable payload if isErrorEnabled is true
 */
inline fun Logger.error(msg: () -> Any?, t: Throwable) {
    if (isErrorEnabled) error(msg.toStringSafe(), t)
}

@Suppress("NOTHING_TO_INLINE")
inline fun (() -> Any?).toStringSafe(): String {
    return try {
        invoke().toString()
    } catch (e: Exception) {
        "KtLogger: get message exception: $e"
    }
}