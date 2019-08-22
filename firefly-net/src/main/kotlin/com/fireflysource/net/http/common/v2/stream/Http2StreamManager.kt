package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.decoder.Parser
import com.fireflysource.net.http.common.v2.frame.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Http2StreamManager(
    initStreamId: Int = 1,
    var maxLocalStreams: Int = -1,
    val local: Boolean = true,
    private val frameSender: FrameSender,
    private val flowControl: FlowControlStrategy
) : Parser.Listener {

    private val streamId = AtomicInteger(initStreamId)
    private val http2StreamMap = ConcurrentHashMap<Int, Http2Stream>()
    private val localStreamCount = AtomicInteger()
    private val sendWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)
    private val recvWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)

    fun newStream(headersFrame: HeadersFrame, listener: Http2StreamListener): Http2Stream {
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


    override fun onData(frame: DataFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHeaders(frame: HeadersFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPriority(frame: PriorityFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onReset(frame: ResetFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSettings(frame: SettingsFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPushPromise(frame: PushPromiseFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPing(frame: PingFrame) {
        if (frame.isReply) {

        }
    }

    override fun onGoAway(frame: GoAwayFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onWindowUpdate(frame: WindowUpdateFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStreamFailure(streamId: Int, error: Int, reason: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailure(error: Int, reason: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}