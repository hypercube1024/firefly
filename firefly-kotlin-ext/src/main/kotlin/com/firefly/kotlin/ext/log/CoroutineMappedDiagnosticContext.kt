package com.firefly.kotlin.ext.log

import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.http.getAttr
import com.firefly.server.http2.router.RoutingContext
import com.firefly.utils.Assert
import com.firefly.utils.log.MappedDiagnosticContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Pengtao Qiu
 */
class CoroutineMappedDiagnosticContext : MappedDiagnosticContext {

    val tracingIdKey = "_coroutineMDCKey"

    private var requestCtx: CoroutineLocal<RoutingContext>? = null
    private var initialized = AtomicBoolean(false)

    init {
        println("new CoroutineMappedDiagnosticContext")
    }

    fun setRequestCtx(reqCtx: CoroutineLocal<RoutingContext>) {
        Assert.notNull(reqCtx, "the request ctx must be not null")
        if (initialized.compareAndSet(false, true)) {
            requestCtx = reqCtx
        }
    }

    fun getContextMap(): MutableMap<String, String>? {
        return requestCtx?.get()?.getAttr(tracingIdKey)
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
        requestCtx?.get()?.setAttribute(tracingIdKey, contextMap)
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