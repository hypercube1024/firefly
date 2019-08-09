package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.v2.frame.GoAwayFrame
import com.fireflysource.net.http.common.v2.frame.PingFrame
import com.fireflysource.net.http.common.v2.frame.PriorityFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame

class DefaultHttp2ClientConnectionListener : Http2ClientConnectionListener {
    
    override fun onPreface(http2ClientConnection: Http2ClientConnection): MutableMap<Int, Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun onNewStream(stream: Http2Stream): Http2StreamListener {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun onPriorityFrame(http2ClientConnection: Http2ClientConnection, frame: PriorityFrame) {
    }

    override suspend fun onSettingsFrame(http2ClientConnection: Http2ClientConnection, frame: SettingsFrame) {
    }

    override suspend fun onPingFrame(http2ClientConnection: Http2ClientConnection, frame: PingFrame) {
    }

    override suspend fun onGoAwayFrame(http2ClientConnection: Http2ClientConnection, frame: GoAwayFrame) {
    }
}