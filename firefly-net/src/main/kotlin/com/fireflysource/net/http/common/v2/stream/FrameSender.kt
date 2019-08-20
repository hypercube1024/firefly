package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.encoder.Generator
import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.Frame
import com.fireflysource.net.tcp.TcpConnection

class FrameSender(
    config: HttpConfig,
    private val tcpConnection: TcpConnection
) {

    private val generator = Generator(config.maxDynamicTableSize, config.maxHeaderBlockFragment)

    fun sendControlFrame(vararg frame: Frame) {
        val bufList = frame.map { generator.control(it).byteBuffers }.flatten()
        tcpConnection.write(bufList, 0, bufList.size, Result.discard())
    }

    fun sendDataFrame(frame: DataFrame, maxLength: Int) {
        val bufList = generator.data(frame, maxLength).byteBuffers
        tcpConnection.write(bufList, 0, bufList.size, Result.discard())
    }

}