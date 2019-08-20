package com.fireflysource.net.http.common.v2.stream

interface FlowControlStrategy {

    fun onStreamCreated(stream: Http2Stream)

}