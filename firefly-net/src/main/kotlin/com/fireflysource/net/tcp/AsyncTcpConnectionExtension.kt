package com.fireflysource.net.tcp

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.coroutine.launchGlobally
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

fun TcpConnection.launch(block: suspend CoroutineScope.() -> Unit): Job = launchGlobally(coroutineDispatcher, block)

fun <T> TcpConnection.async(block: suspend CoroutineScope.() -> T): Deferred<T> =
    asyncGlobally(coroutineDispatcher, block)

fun TcpConnection.launchWithAttr(
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> Unit
): Job = com.fireflysource.common.coroutine.launchWithAttr(coroutineDispatcher, attributes, block)

fun <T> TcpConnection.asyncWithAttr(
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = com.fireflysource.common.coroutine.asyncWithAttr(coroutineDispatcher, attributes, block)