package com.firefly.kotlin.ext.common

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.ContinuationInterceptor

/**
 * Retain data in the coroutine lifecycle.
 *
 * @author Pengtao Qiu
 */
class CoroutineLocal<D> {

    private val threadLocal = ThreadLocal<D>()

    fun asElement(d: D) = threadLocal.asContextElement(d)

    fun get(): D? = threadLocal.get()

    fun set(value: D?) = threadLocal.set(value)
}

object CoroutineLocalContext {

    private val ctx: CoroutineLocal<MutableMap<String, Any>> by lazy {
        CoroutineLocal<MutableMap<String, Any>>()
    }

    fun asElement(attributes: MutableMap<String, Any>): ThreadContextElement<MutableMap<String, Any>> {
        return if (attributes is ConcurrentHashMap) {
            ctx.asElement(attributes)
        } else {
            ctx.asElement(ConcurrentHashMap(attributes))
        }
    }

    fun inheritParentElement(attributes: MutableMap<String, Any>? = null): ThreadContextElement<MutableMap<String, Any>> {
        val parentAttributes = getAttributes()
        val attrs = if (parentAttributes.isNullOrEmpty()) {
            attributes ?: mutableMapOf()
        } else {
            if (!attributes.isNullOrEmpty()) {
                parentAttributes.putAll(attributes)
            }
            parentAttributes
        }
        return asElement(attrs)
    }

    fun getAttributes(): MutableMap<String, Any>? = ctx.get()

    inline fun <reified T> getAttr(key: String): T? {
        val value = getAttributes()?.get(key)
        return if (value == null) null else value as T
    }

    inline fun <reified T> getAttrOrDefault(key: String, func: (String) -> T): T {
        val value = getAttributes()?.get(key)
        return if (value == null) func.invoke(key) else value as T
    }

    inline fun <reified T : Any> setAttr(key: String, value: T): T? {
        val oldValue = getAttributes()?.put(key, value)
        return if (oldValue == null) null else oldValue as T
    }

    inline fun <reified T : Any> computeIfAbsent(key: String, crossinline func: (String) -> T): T? {
        val value = getAttributes()?.computeIfAbsent(key) {
            func.invoke(it)
        }
        return if (value == null) null else value as T
    }
}

fun <T> asyncTraceable(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    block: suspend CoroutineScope.() -> T
                      ): Deferred<T> {
    return GlobalScope.async(context + CoroutineLocalContext.inheritParentElement()) { block.invoke(this) }
}