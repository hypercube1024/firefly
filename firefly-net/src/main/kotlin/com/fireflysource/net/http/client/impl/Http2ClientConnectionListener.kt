package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.v2.frame.GoAwayFrame
import com.fireflysource.net.http.common.v2.frame.PingFrame
import com.fireflysource.net.http.common.v2.frame.PriorityFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.http.common.v2.stream.Http2Stream
import com.fireflysource.net.http.common.v2.stream.Http2StreamListener

interface Http2ClientConnectionListener {

    fun onPreface(http2ClientConnection: Http2ClientConnection): MutableMap<Int, Int>

    suspend fun onNewStream(stream: Http2Stream): Http2StreamListener

    suspend fun onPriorityFrame(http2ClientConnection: Http2ClientConnection, frame: PriorityFrame)

    suspend fun onSettingsFrame(http2ClientConnection: Http2ClientConnection, frame: SettingsFrame)

    suspend fun onPingFrame(http2ClientConnection: Http2ClientConnection, frame: PingFrame)

    suspend fun onGoAwayFrame(http2ClientConnection: Http2ClientConnection, frame: GoAwayFrame)
}