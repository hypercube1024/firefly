package com.fireflysource.common.slf4j

import com.fireflysource.common.reflect.KotlinNameResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Pengtao Qiu
 */
object LazyLoggerKt {
    fun getLogger(func: () -> Unit): Logger = LoggerFactory.getLogger(KotlinNameResolver.name(func))

    fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)

    fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
}

/**
 * Lazy add a log message if isTraceEnabled is true
 */
inline fun Logger.trace(crossinline msg: () -> Any?) {
    if (isTraceEnabled) trace(toStringSafe(msg))
}

/**
 * Lazy add a log message if isDebugEnabled is true
 */
inline fun Logger.debug(crossinline msg: () -> Any?) {
    if (isDebugEnabled) debug(toStringSafe(msg))
}

/**
 * Lazy add a log message if isInfoEnabled is true
 */
inline fun Logger.info(crossinline msg: () -> Any?) {
    if (isInfoEnabled) info(toStringSafe(msg))
}

/**
 * Lazy add a log message if isWarnEnabled is true
 */
inline fun Logger.warn(crossinline msg: () -> Any?) {
    if (isWarnEnabled) warn(toStringSafe(msg))
}

/**
 * Lazy add a log message if isErrorEnabled is true
 */
inline fun Logger.error(crossinline msg: () -> Any?) {
    if (isErrorEnabled) error(toStringSafe(msg))
}

/**
 * Lazy add a log message with throwable payload if isTraceEnabled is true
 */
inline fun Logger.trace(throwable: Throwable, crossinline msg: () -> Any?) {
    if (isTraceEnabled) trace(toStringSafe(msg), throwable)
}

/**
 * Lazy add a log message with throwable payload if isDebugEnabled is true
 */
inline fun Logger.debug(throwable: Throwable, crossinline msg: () -> Any?) {
    if (isDebugEnabled) debug(toStringSafe(msg), throwable)
}

/**
 * Lazy add a log message with throwable payload if isInfoEnabled is true
 */
inline fun Logger.info(throwable: Throwable, crossinline msg: () -> Any?) {
    if (isInfoEnabled) info(toStringSafe(msg), throwable)
}

/**
 * Lazy add a log message with throwable payload if isWarnEnabled is true
 */
inline fun Logger.warn(throwable: Throwable, crossinline msg: () -> Any?) {
    if (isWarnEnabled) warn(toStringSafe(msg), throwable)
}

/**
 * Lazy add a log message with throwable payload if isErrorEnabled is true
 */
inline fun Logger.error(throwable: Throwable, crossinline msg: () -> Any?) {
    if (isErrorEnabled) error(toStringSafe(msg), throwable)
}


inline fun toStringSafe(crossinline msg: () -> Any?): String {
    return try {
        msg.invoke().toString()
    } catch (e: Exception) {
        "KtLogger: get message exception: $e"
    }
}