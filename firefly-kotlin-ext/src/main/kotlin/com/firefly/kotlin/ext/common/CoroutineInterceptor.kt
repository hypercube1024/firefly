package com.firefly.kotlin.ext.common

import com.firefly.kotlin.ext.log.Log
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Maintain data in the coroutine lifecycle.
 * @author Pengtao Qiu
 */

private val log = Log.getLogger("firefly-system")

class CoroutineLocal<D> {

    private val threadLocal = ThreadLocal<D>()

    fun createContext(data: D, context: ContinuationInterceptor = CommonCoroutinePool): ContinuationInterceptor = InterceptingContext(context, data, threadLocal)

    fun get(): D? = threadLocal.get()
}

class InterceptingContext<D>(val delegateInterceptor: ContinuationInterceptor,
                             val data: D,
                             val threadLocal: ThreadLocal<D>) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
            = delegateInterceptor.interceptContinuation(WrappedContinuation(continuation,
            { threadLocal.set(data) },
            { threadLocal.remove() }))
}

class WrappedContinuation<in T>(val continuation: Continuation<T>,
                                val preBlock: () -> Unit,
                                val postBlock: () -> Unit) : Continuation<T> {

    override val context: CoroutineContext
        get() = continuation.context

    override fun resume(value: T) {
        log.debug("thread resume")
        preBlock()
        try {
            continuation.resume(value)
        } finally {
            postBlock()
        }
    }

    override fun resumeWithException(exception: Throwable) {
        log.debug("thread resume with exception")
        preBlock()
        try {
            continuation.resumeWithException(exception)
        } finally {
            postBlock()
        }
    }

}