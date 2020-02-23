package com.fireflysource.net.http.common.model

fun HttpFields.containExpectContinue(): Boolean {
    val expectValue = this[HttpHeader.EXPECT]
    return HttpHeaderValue.CONTINUE.`is`(expectValue)
}

fun HttpFields.containCloseConnection(version: HttpVersion): Boolean = when (version) {
    HttpVersion.HTTP_0_9, HttpVersion.HTTP_1_0 -> !this.contains(
        HttpHeader.CONNECTION,
        HttpHeaderValue.KEEP_ALIVE.value
    )
    else -> this.contains(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
}