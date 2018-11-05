package com.firefly.kotlin.ext.common

import com.firefly.kotlin.ext.common.CoroutineDispatchers.computation
import com.firefly.kotlin.ext.log.KtLogger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Maintain data in the coroutine lifecycle.
 * @author Pengtao Qiu
 */

private val log = KtLogger.getLogger("firefly-system")

class CoroutineLocal<D> {

    private val threadLocal = ThreadLocal<D>()

    fun createContext(data: D, context: ContinuationInterceptor = computation): ContinuationInterceptor =
        InterceptingContext(context, data, threadLocal)

    fun get(): D? = threadLocal.get()
}

class InterceptingContext<D>(
    val delegateInterceptor: ContinuationInterceptor,
    val data: D,
    val threadLocal: ThreadLocal<D>
                            ) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        delegateInterceptor.interceptContinuation(
            WrappedContinuation(
                continuation,
                { threadLocal.set(data) },
                { threadLocal.remove() })
                                                 )
}

class WrappedContinuation<in T>(
    val continuation: Continuation<T>,
    val preBlock: () -> Unit,
    val postBlock: () -> Unit
                               ) : Continuation<T> {

    override val context: CoroutineContext
        get() = continuation.context

    override fun resumeWith(result: Result<T>) {
        preBlock()
        try {
            log.debug("thread resume")
            continuation.resumeWith(result)
        } finally {
            postBlock()
        }
    }

}