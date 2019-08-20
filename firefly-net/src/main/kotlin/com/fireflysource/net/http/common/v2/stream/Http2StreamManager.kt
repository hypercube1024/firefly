package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.PriorityFrame
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Http2StreamManager(
    initStreamId: Int = 1,
    var maxLocalStreams: Int = -1,
    val local: Boolean = true,
    private val frameSender: FrameSender,
    private val flowControl: FlowControlStrategy
) {

    private val streamId = AtomicInteger(initStreamId)
    private val http2StreamMap = ConcurrentHashMap<Int, Http2Stream>()
    private val localStreamCount = AtomicInteger()

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
            frameSender.sendControlFrame(newHeadersFrame)
            return stream
        } else {
            val stream = createLocalStream(frameStreamId, listener)
            frameSender.sendControlFrame(headersFrame)
            return stream
        }
    }

    private fun getNextStreamId(): Int = streamId.getAndAdd(2)

    private fun createLocalStream(id: Int, listener: Http2StreamListener): Http2Stream {
        return http2StreamMap.computeIfAbsent(id) {
            checkMaxLocalStreams()
            val stream = Http2Stream(id, listener)
            flowControl.onStreamCreated(stream)
            stream
        }
    }

    private fun checkMaxLocalStreams() {
        while (true) {
            val localCount = localStreamCount.get()
            val maxCount = maxLocalStreams
            if (maxCount in 0..localCount) {
                throw IllegalStateException("Max local stream count $localCount exceeded $maxCount")
            }
            if (localStreamCount.compareAndSet(localCount, localCount + 1)) {
                break
            }
        }
    }

}