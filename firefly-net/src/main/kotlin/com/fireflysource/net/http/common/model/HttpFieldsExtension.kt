package com.fireflysource.net.http.common.model

import com.fireflysource.net.http.common.model.HttpHeader.CONNECTION
import com.fireflysource.net.http.common.model.HttpHeader.EXPECT
import com.fireflysource.net.http.common.model.HttpHeaderValue.CLOSE
import com.fireflysource.net.http.common.model.HttpHeaderValue.CONTINUE
import com.fireflysource.net.http.common.model.HttpVersion.HTTP_0_9
import com.fireflysource.net.http.common.model.HttpVersion.HTTP_1_0

fun HttpFields.expectServerAcceptsContent(): Boolean {
    return this.contains(EXPECT, CONTINUE.value)
}

fun HttpFields.isCloseConnection(version: HttpVersion): Boolean = when (version) {
    HTTP_0_9, HTTP_1_0 -> !this.contains(CONNECTION, HttpHeaderValue.KEEP_ALIVE.value)
    else -> this.contains(CONNECTION, CLOSE.value)
}