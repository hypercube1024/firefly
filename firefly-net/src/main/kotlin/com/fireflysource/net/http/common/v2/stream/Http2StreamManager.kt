package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.net.http.common.v2.frame.Frame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.PriorityFrame
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Http2StreamManager(
    initStreamId: Int = 1,
    val local: Boolean = true
) {

    private val streamId = AtomicInteger(initStreamId)
    private val http2StreamMap = ConcurrentHashMap<Int, Http2Stream>()

    suspend fun newStream(headersFrame: HeadersFrame, listener: Http2StreamListener): Http2Stream {
        val frameStreamId = headersFrame.streamId
        if (frameStreamId <= 0) {
            val nextStreamId = getNextStreamId()
            val priority = if (headersFrame.priority == null) {
                null
            } else {
                PriorityFrame(
                    nextStreamId,
                    headersFrame.priority.parentStreamId,
                    headersFrame.priority.weight,
                    headersFrame.priority.isExclusive
                )
            }
            val newHeadersFrame = HeadersFrame(nextStreamId, headersFrame.metaData, priority, headersFrame.isEndStream)
            val stream = createLocalStream(nextStreamId, listener)
            sendControlFrame(newHeadersFrame)
            return stream
        } else {
            val stream = createLocalStream(frameStreamId, listener)
            sendControlFrame(headersFrame)
            return stream
        }
    }

    private fun getNextStreamId(): Int = streamId.getAndAdd(2)

    private fun createLocalStream(id: Int, listener: Http2StreamListener): Http2Stream {
        TODO("not implement")
    }

    private fun sendControlFrame(vararg frame: Frame) {
        TODO("not implement")
    }
}