package com.firefly.kotlin.ext.log

import com.firefly.kotlin.ext.common.CoroutineLocalContext
import com.firefly.utils.log.MappedDiagnosticContext
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Pengtao Qiu
 */
class CoroutineMappedDiagnosticContext : MappedDiagnosticContext {

    val tracingIdKey = "_coroutineMDCKey"

    init {
        println("new CoroutineMappedDiagnosticContext")
    }

    private fun getContextMap(): MutableMap<String, String>? {
        return CoroutineLocalContext.computeIfAbsent(tracingIdKey) { ConcurrentHashMap<String, String>() }
    }

    override fun clear() {
        getContextMap()?.clear()
    }

    override fun getCopyOfContextMap(): MutableMap<String, String> {
        val map = getContextMap()
        return map?.toMutableMap() ?: mutableMapOf()
    }

    override fun put(key: String, value: String) {
        var map = getContextMap()
        if (map == null) {
            map = mutableMapOf()
            setContextMap(map)
        }
        map[key] = value
    }

    override fun setContextMap(contextMap: MutableMap<String, String>) {
        if (contextMap is ConcurrentHashMap) {
            CoroutineLocalContext.getAttributes()?.put(tracingIdKey, contextMap)
        } else {
            CoroutineLocalContext.getAttributes()?.put(tracingIdKey, ConcurrentHashMap(contextMap))
        }
    }

    override fun remove(key: String) {
        getContextMap()?.remove(key)
    }

    override fun get(key: String): String? {
        return getContextMap()?.get(key)
    }

    override fun getKeys(): MutableSet<String>? {
        return getContextMap()?.keys
    }

}