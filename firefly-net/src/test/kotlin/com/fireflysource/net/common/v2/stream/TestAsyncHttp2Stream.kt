package com.fireflysource.net.common.v2.stream

import com.fireflysource.common.sys.Result.discard
import com.fireflysource.net.http.common.v2.frame.CloseState
import com.fireflysource.net.http.common.v2.frame.ErrorCode
import com.fireflysource.net.http.common.v2.frame.ResetFrame
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Stream
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.common.v2.stream.getAndIncreaseStreamId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class TestAsyncHttp2Stream {

    private val asyncHttp2Connection = Mockito.mock(AsyncHttp2Connection::class.java)

    @Test
    @DisplayName("should update stream window successfully")
    fun testWindowUpdate() {
        val stream = AsyncHttp2Stream(asyncHttp2Connection, 1, true, Stream.Listener.Adapter())
        val initRecvWindow = stream.updateRecvWindow(25)
        assertEquals(0, initRecvWindow)
        assertEquals(25, stream.getRecvWindow())

        val initSendWindow = stream.updateSendWindow(13)
        assertEquals(0, initSendWindow)
        assertEquals(13, stream.getSendWindow())
    }

    @Test
    @DisplayName("should close stream after the stream remote close and local close")
    fun testCloseStream() {
        val stream = AsyncHttp2Stream(asyncHttp2Connection, 1, true, Stream.Listener.Adapter())
        assertFalse(stream.isClosed)

        val result = stream.updateClose(true, CloseState.Event.RECEIVED)
        assertFalse(result)

        val result1 = stream.updateClose(true, CloseState.Event.BEFORE_SEND)
        assertFalse(result1)

        val result2 = stream.updateClose(true, CloseState.Event.AFTER_SEND)
        assertTrue(result2)
        assertTrue(stream.isClosed)
    }

    @Test
    @DisplayName("should stream reset when the reset frame sent")
    fun testReset() {
        val stream = AsyncHttp2Stream(asyncHttp2Connection, 1, true, Stream.Listener.Adapter())
        assertFalse(stream.isReset)

        val frame = ResetFrame(1, ErrorCode.INTERNAL_ERROR.code)
        val future = CompletableFuture<Long>()
        future.complete(0)
        `when`(asyncHttp2Connection.sendControlFrame(stream, frame)).thenReturn(future)
        stream.reset(frame, discard())
        verify(asyncHttp2Connection).sendControlFrame(stream, frame)
        assertTrue(stream.isReset)
    }

    @Test
    @DisplayName("should get the current id and increase 2 and skip 0")
    fun testStreamIdIncrease() {
        val id = AtomicInteger(-4)
        assertEquals(-4, getAndIncreaseStreamId(id))
        assertEquals(-2, getAndIncreaseStreamId(id))
        assertEquals(2, getAndIncreaseStreamId(id))

        val id2 = AtomicInteger(-3)
        assertEquals(-3, getAndIncreaseStreamId(id2))
        assertEquals(-1, getAndIncreaseStreamId(id2))
        assertEquals(1, getAndIncreaseStreamId(id2))
    }
}