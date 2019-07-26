package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.PushPromiseFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame

interface Http2StreamListener {

    suspend fun onHeadersFrame(stream: Http2Stream, frame: HeadersFrame)

    suspend fun onPushPromiseFrame(stream: Http2Stream, frame: PushPromiseFrame)

    suspend fun onDataFrame(stream: Http2Stream, frame: DataFrame)

    suspend fun onResetFrame(stream: Http2Stream, frame: ResetFrame)
}