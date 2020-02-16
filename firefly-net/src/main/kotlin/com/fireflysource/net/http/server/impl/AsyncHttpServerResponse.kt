package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerContentProvider
import com.fireflysource.net.http.server.HttpServerOutputChannel
import com.fireflysource.net.http.server.HttpServerResponse
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class AsyncHttpServerResponse(private val httpServerConnection: HttpServerConnection) : HttpServerResponse {

    val response: MetaData.Response = MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.OK_200, HttpFields())
    private var contentProvider: HttpServerContentProvider? = null
    private var cookieList: List<Cookie>? = null
    private val committed = AtomicBoolean(false)
    private var serverOutputChannel: HttpServerOutputChannel? = null

    override fun getStatus(): Int = response.status

    override fun setStatus(status: Int) {
        response.status = status
    }

    override fun getReason(): String = response.reason

    override fun setReason(reason: String) {
        response.reason = reason
    }

    override fun getHttpVersion(): HttpVersion = response.httpVersion

    override fun setHttpVersion(httpVersion: HttpVersion) {
        response.httpVersion = httpVersion
    }

    override fun getHttpFields(): HttpFields = response.fields

    override fun setHttpFields(httpFields: HttpFields) {
        response.fields.clear()
        response.fields.addAll(httpFields)
    }

    override fun getCookies(): List<Cookie> {
        val cookies = cookieList
        return if (cookies == null) {
            val newCookies = LinkedList<Cookie>()
            cookieList = newCookies
            newCookies
        } else cookies
    }

    override fun setCookies(cookies: List<Cookie>) {
        cookieList = cookies
    }

    override fun getContentProvider(): HttpServerContentProvider? = contentProvider

    override fun setContentProvider(contentProvider: HttpServerContentProvider) {
        Assert.state(!isCommitted, "Set content provider must before commit response.")
        this.contentProvider = contentProvider
    }

    override fun getTrailerSupplier(): Supplier<HttpFields> = response.trailerSupplier

    override fun setTrailerSupplier(supplier: Supplier<HttpFields>) {
        response.trailerSupplier = supplier
    }

    override fun isCommitted(): Boolean = committed.get()

    override fun commit(): CompletableFuture<Void> = httpServerConnection.coroutineScope
        .launch { commitResponse() }
        .asCompletableFuture()
        .thenCompose { Result.DONE }

    private suspend fun commitResponse() {
        if (committed.compareAndSet(false, true)) {
            val outputChannel = httpServerConnection.createHttpServerOutputChannel()
            outputChannel.commit().await()
            serverOutputChannel = outputChannel
            val provider = contentProvider
            if (provider != null) {
                try {
                    writeContent(provider, outputChannel)
                } finally {
                    outputChannel.closeFuture().await()
                }
            }
        }
    }

    private suspend fun writeContent(provider: HttpServerContentProvider, outputChannel: HttpServerOutputChannel) {
        writeLoop@ while (true) {
            val size = if (provider.length() > 0) provider.length().coerceAtMost(4096L).toInt() else 4096
            val buffer = BufferUtils.allocate(size)
            val position = buffer.flipToFill()
            val length = provider.read(buffer).await()
            buffer.flipToFlush(position)
            when {
                length > 0 -> outputChannel.write(buffer).await()
                length < 0 -> break@writeLoop
            }
        }
        provider.closeFuture().await()
    }

    override fun getOutputChannel(): HttpServerOutputChannel {
        val outputChannel = serverOutputChannel
        if (outputChannel == null) {
            throw IllegalStateException("The response not commit")
        } else {
            Assert.state(
                contentProvider == null,
                "The content provider is not null. The server has used content provider to output content."
            )
            return outputChannel
        }
    }

    fun reset() {
        response.recycle()
        contentProvider = null
        cookieList = null
    }

}