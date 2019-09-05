package com.fireflysource.common.coroutine

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.ContinuationInterceptor

/**
 * Retain a value in the coroutine scope.
 *
 * @author Pengtao Qiu
 */
class CoroutineLocal<T> {

    private val threadLocal = ThreadLocal<T>()

    /**
     * Convert a value to the coroutine context element.
     *
     * @param value A value runs through in the coroutine scope.
     * @return The coroutine context element.
     */
    fun asElement(value: T) = threadLocal.asContextElement(value)

    /**
     * Get the value in the coroutine scope.
     *
     * @return The value in the coroutine scope.
     */
    fun get(): T? = threadLocal.get()

    /**
     * Set the value in the coroutine scope.
     *
     * @param value The value in the coroutine scope.
     */
    fun set(value: T?) = threadLocal.set(value)
}

/**
 * Retain the attributes in the coroutine scope.
 *
 * @author Pengtao Qiu
 */
object CoroutineLocalContext {

    private val ctx: CoroutineLocal<MutableMap<String, Any>> by lazy {
        CoroutineLocal<MutableMap<String, Any>>()
    }

    /**
     * Convert the attributes to the coroutine context element.
     *
     * @param attributes The attributes runs through in the coroutine scope.
     * @return The coroutine context element.
     */
    fun asElement(attributes: MutableMap<String, Any>): ThreadContextElement<MutableMap<String, Any>> {
        return if (attributes is ConcurrentHashMap) {
            ctx.asElement(attributes)
        } else {
            ctx.asElement(ConcurrentHashMap(attributes))
        }
    }

    /**
     * Merge the attributes into the parent coroutine context element.
     *
     * @param attributes The attributes are merged into the parent coroutine context element.
     * @return The coroutine context element.
     */
    fun inheritParentElement(attributes: MutableMap<String, Any>? = null): ThreadContextElement<MutableMap<String, Any>> {
        val parentAttributes = getAttributes()
        val attrs = if (parentAttributes.isNullOrEmpty()) {
            attributes ?: ConcurrentHashMap()
        } else {
            if (!attributes.isNullOrEmpty()) {
                parentAttributes.putAll(attributes)
            }
            parentAttributes
        }
        return asElement(attrs)
    }

    /**
     * Get the current attributes.
     *
     * @return The current attributes.
     */
    fun getAttributes(): MutableMap<String, Any>? = ctx.get()

    /**
     * Get an attribute in the current coroutine scope.
     *
     * @param name The attribute name.
     * @return An attribute in the current coroutine scope.
     */
    inline fun <reified T> getAttr(name: String): T? {
        val value = getAttributes()?.get(name)
        return if (value == null) null else value as T
    }

    /**
     * Get an attribute in the current coroutine scope, if the value is null return the default value.
     *
     * @param name The attribute name.
     * @param func Get the default value lazily.
     * @return An attribute in the current coroutine scope or the default value.
     */
    inline fun <reified T> getAttrOrDefault(name: String, func: (String) -> T): T {
        val value = getAttributes()?.get(name)
        return if (value == null) func.invoke(name) else value as T
    }

    /**
     * Set an attribute in the current coroutine scope.
     *
     * @param name The attribute's name.
     * @param value The attribute's value.
     * @return The old value in the current coroutine scope.
     */
    inline fun <reified T : Any> setAttr(name: String, value: T): T? {
        val oldValue = getAttributes()?.put(name, value)
        return if (oldValue == null) null else oldValue as T
    }

    /**
     * If the specified attribute name is not already associated with a value (or is mapped
     * to null), attempts to compute its value using the given mapping
     * function and enters it into this map unless null.
     *
     * @param name The attribute's name.
     * @param func The mapping function.
     * @return The value in the current coroutine scope.
     */
    inline fun <reified T : Any> computeIfAbsent(name: String, crossinline func: (String) -> T): T? {
        val value = getAttributes()?.computeIfAbsent(name) {
            func.invoke(it)
        }
        return if (value == null) null else value as T
    }
}

/**
 * Starts an asynchronous task and inherits parent coroutine local attributes.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param attributes The attributes are merged into the parent coroutine context element.
 * @param block The coroutine code block.
 * @return The deferred task result.
 */
fun <T> asyncWithAttr(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return GlobalScope.async(context + CoroutineLocalContext.inheritParentElement(attributes)) { block.invoke(this) }
}

fun <T> asyncGlobally(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return GlobalScope.async(context) { block.invoke(this) }
}

/**
 * Starts an asynchronous task without the return value and inherits parent coroutine local attributes.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param attributes The attributes are merged into the parent coroutine context element.
 * @param block The coroutine code block.
 * @return The current job.
 */
fun launchWithAttr(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return GlobalScope.launch(context + CoroutineLocalContext.inheritParentElement(attributes)) { block.invoke(this) }
}

fun launchGlobally(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return GlobalScope.launch(context) { block.invoke(this) }
}

/**
 * Starts an asynchronous task waiting the result and inherits parent coroutine local attributes.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param attributes The attributes are merged into the parent coroutine context element.
 * @param block The coroutine code block.
 * @return The result.
 */
suspend fun <T> withAttr(
    context: ContinuationInterceptor = CoroutineDispatchers.computation,
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> T
): T {
    return withContext(context + CoroutineLocalContext.inheritParentElement(attributes)) { block.invoke(this) }
}