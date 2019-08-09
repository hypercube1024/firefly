package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.PushPromiseFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame

class Http2Stream(
    val id: Int,
    val listener: Http2StreamListener
) {

    private var closed: Boolean = false

    suspend fun sendHeadersFrame(frame: HeadersFrame) {
        TODO("not implemented")
    }

    suspend fun pushPromiseFrame(frame: PushPromiseFrame) {
        TODO("not implemented")
    }

    suspend fun sendDataFrame(frame: DataFrame) {
        TODO("not implemented")
    }

    suspend fun sendResetFrame(frame: ResetFrame) {
        TODO("not implemented")
    }

    fun isClosed() = closed
}