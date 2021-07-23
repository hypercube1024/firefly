package com.fireflysource.common.coroutine

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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

    private val ctx: CoroutineLocal<MutableMap<String, Any>> by lazy { CoroutineLocal() }

    /**
     * Convert the attributes to the coroutine context element.
     *
     * @param attributes The attributes run through in the coroutine scope.
     * @return The coroutine context element.
     */
    fun asElement(attributes: MutableMap<String, Any>): ThreadContextElement<MutableMap<String, Any>> =
        ctx.asElement(HashMap(attributes))

    /**
     * Inherit parent coroutine context element, and merge it into current attributes.
     *
     * @param attributes The attributes run through in the coroutine scope.
     * @return The coroutine context element.
     */
    fun inheritParentElement(attributes: MutableMap<String, Any>? = null): ThreadContextElement<MutableMap<String, Any>> {
        val newAttributes = HashMap<String, Any>()
        val parentAttributes = getAttributes()
        if (parentAttributes != null) {
            newAttributes.putAll(parentAttributes)
        }
        if (attributes != null) {
            newAttributes.putAll(attributes)
        }
        return ctx.asElement(newAttributes)
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
     * @param name The name of attribute.
     * @return An attribute in the current coroutine scope.
     */
    inline fun <reified T> getAttr(name: String): T? {
        return getAttributes()?.get(name) as T?
    }

    /**
     * Get an attribute in the current coroutine scope, if the value is null return the default value.
     *
     * @param name The name of attribute.
     * @param func Get the default value lazily.
     * @return An attribute in the current coroutine scope, or the default value.
     */
    inline fun <reified T> getAttrOrDefault(name: String, crossinline func: (String) -> T): T {
        return getAttributes()?.get(name) as T? ?: func(name)
    }

    /**
     * Set an attribute in the current coroutine scope.
     *
     * @param name The attribute's name.
     * @param value The attribute's value.
     * @return The old value in the current coroutine scope.
     */
    inline fun <reified T : Any> setAttr(name: String, value: T): T? {
        return getAttributes()?.put(name, value) as T?
    }

    /**
     * If the specified attribute name does not already associate with a value (or is mapped
     * to null), attempts to compute its value using the given mapping
     * function and enters it into this map unless null.
     *
     * @param name The attribute's name.
     * @param func The mapping function.
     * @return The value in the current coroutine scope.
     */
    inline fun <reified T : Any> computeIfAbsent(name: String, crossinline func: (String) -> T): T? {
        return getAttributes()?.computeIfAbsent(name) { func(it) } as T?
    }
}

inline fun <T> CoroutineScope.inheritableAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    attributes: MutableMap<String, Any>? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline block: suspend CoroutineScope.() -> T
): Deferred<T> {
    return this.async(context + CoroutineLocalContext.inheritParentElement(attributes), start) { block(this) }
}

inline fun CoroutineScope.inheritableLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    attributes: MutableMap<String, Any>? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline block: suspend CoroutineScope.() -> Unit
): Job {
    return this.launch(context + CoroutineLocalContext.inheritParentElement(attributes), start) { block(this) }
}

/**
 * Starts an asynchronous task waiting the result and inherits parent coroutine local attributes.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param attributes The attributes merge into the parent coroutine context element.
 * @param block The coroutine code block.
 * @return The result.
 */
suspend inline fun <T> withContextInheritable(
    context: CoroutineContext = EmptyCoroutineContext,
    attributes: MutableMap<String, Any>? = null,
    crossinline block: suspend CoroutineScope.() -> T
): T {
    return withContext(context + CoroutineLocalContext.inheritParentElement(attributes)) { block(this) }
}