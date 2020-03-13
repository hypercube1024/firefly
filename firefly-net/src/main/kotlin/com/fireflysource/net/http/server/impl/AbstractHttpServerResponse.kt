package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.codec.CookieGenerator
import com.fireflysource.net.http.common.exception.HttpServerResponseNotCommitException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerContentProvider
import com.fireflysource.net.http.server.HttpServerOutputChannel
import com.fireflysource.net.http.server.HttpServerResponse
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

abstract class AbstractHttpServerResponse(private val httpServerConnection: HttpServerConnection) : HttpServerResponse {

    val response: MetaData.Response = MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.OK_200, HttpFields())
    private var contentProvider: HttpServerContentProvider? = null
    private var cookieList: List<Cookie>? = null
    private val committed = AtomicBoolean(false)
    private var serverOutputChannel: HttpServerOutputChannel? = null
    private val mutex = Mutex()

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

    override fun getCookies(): List<Cookie> = cookieList ?: listOf()

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
        .launch { commitAwait() }
        .asCompletableFuture()
        .thenCompose { Result.DONE }

    private suspend fun commitAwait() {
        if (committed.get()) return

        mutex.withLock {
            if (committed.get()) return@commitAwait

            createOutputChannelAndCommit()
            committed.set(true)
        }
    }

    private suspend fun createOutputChannelAndCommit() {
        if (response.fields[HttpHeader.CONNECTION] == null && httpVersion == HttpVersion.HTTP_1_1) {
            response.fields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE)
        }

        cookies.map { CookieGenerator.generateSetCookie(it) }
            .forEach { response.fields.add(HttpHeader.SET_COOKIE, it) }

        val provider = contentProvider
        if (provider != null && provider.length() >= 0) {
            response.fields.put(HttpHeader.CONTENT_LENGTH, provider.length().toString())
        }

        val output = createHttpServerOutputChannel(response)
        if (provider != null) {
            output.useAwait {
                it.commit().await()
                writeContent(provider, it)
            }
        } else {
            output.commit().await()
        }
        this.serverOutputChannel = output
    }

    /**
     * Create the HTTP server output channel. It outputs the HTTP response.
     *
     * @return The HTTP server output channel.
     */
    abstract fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel

    private suspend fun writeContent(provider: HttpServerContentProvider, outputChannel: HttpServerOutputChannel) {
        writeLoop@ while (true) {
            val size = if (provider.length() > 0) provider.length().coerceAtMost(8192L).toInt() else 8192
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
            throw HttpServerResponseNotCommitException("The response not commit")
        } else {
            Assert.state(
                contentProvider == null,
                "The content provider is not null. The server has used content provider to output content."
            )
            return outputChannel
        }
    }

    override fun closeFuture(): CompletableFuture<Void> {
        val provider = contentProvider
        return if (provider == null) {
            httpServerConnection.coroutineScope.launch {
                commit().await()
                outputChannel.closeFuture().await()
            }.asCompletableFuture().thenCompose { Result.DONE }
        } else Result.DONE
    }

    override fun close() {
        closeFuture()
    }
}