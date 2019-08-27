package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.PushPromiseFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame
import java.util.function.Consumer

class AsyncHttp2Stream(
    private val id: Int,
    private val local: Boolean,
    private val listener: Stream.Listener
) : Stream {

    override fun getId(): Int = id

    override fun getHttp2Connection(): Http2Connection {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun headers(frame: HeadersFrame?, result: Consumer<Result<Void>>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun push(frame: PushPromiseFrame?, promise: Consumer<Result<Stream>>?, listener: Stream.Listener?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun data(frame: DataFrame?, result: Consumer<Result<Void>>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset(frame: ResetFrame?, result: Consumer<Result<Void>>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAttribute(key: String?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAttribute(key: String?, value: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAttribute(key: String?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isReset(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isClosed(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getIdleTimeout(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setIdleTimeout(idleTimeout: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}